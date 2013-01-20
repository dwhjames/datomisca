import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomic.Connection
import datomic.Database
import datomic.Peer
import datomic.Util

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import java.io.Reader
import java.io.FileReader

import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import reactivedatomic._
import Datomic._

@RunWith(classOf[JUnitRunner])
class DatomicDemoSpec extends Specification {
  "Datomic" should {
    "create simple schema and provision data" in {
      import scala.concurrent.ExecutionContext.Implicits.global

      val uri = "datomic:mem://DatomicDemoSpec"

      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, Datomic.createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val clever = AddIdent( Keyword(person.character, "clever") )
      val violent = AddIdent( person.character / "violent")
      val weak = AddIdent( KW(":person.character/weak") )

      // erase leading ':' to see the error
      val dumb = AddIdent( KW(":person.character/dumb") )

      /* Programmatic creation of a Schema */
      val schema = Seq(
        Attribute( KW(":person/name"), SchemaType.string, Cardinality.one ).withDoc("Person's name"),
        Attribute( KW(":person/age"), SchemaType.long, Cardinality.one ).withDoc("Person's age"),
        Attribute( KW(":person/character"), SchemaType.ref, Cardinality.many ).withDoc("Person's characters"),
        violent,
        weak,
        clever,
        dumb
      )

      implicit val conn = Datomic.connect(uri)

      /* reactive flow :
       *  - schema creation, 
       *  - provisioning of data
       *  - query 
       */
      val fut = conn.transact(schema).flatMap{ tx => 
        println("Provisioned schema... TX:%s".format(tx))

        /* AddEntity different syntaxes from most programmatic to macrocompiled using inline variables 
         * POTENTIAL DEMO :
         *  - remove a ] from addEntity to show compiling error
         */
        conn.transact(
          AddEntity(DId(Partition.USER))(
            person / "name" -> DString("toto"),
            person / "age" -> DLong(30L),
            person / "character" -> DSet(weak.ident, dumb.ident)
          ),
          Entity.add(DId(Partition.USER))(
            KW(":person/name") -> "tata",
            KW(":person/age") -> 54L,
            KW(":person/character") -> Seq(violent, clever)
          ),
          Entity.add("""{
            :db/id ${DId(Partition.USER)}
            :person/name "tutu"
            :person/age 35
            :person/character [ $weak $dumb ]
          }""")
        ).map{ tx => 
          println("Provisioned data... TX:%s".format(tx))

          /* Query demo 
           * POTENTIAL TESTS:
           *  - remove one square bracket or parenthesis to show compiling error at right place in query
           *  - change Input Args2 to Args3 to show compiling error (beginning of query)
           *  - erase ?a to show compiling error in query (beginning of query)
           */
          val l1 = Datomic.q(Query.manual[Args2, Args3]("""
            [ 
              :find ?e ?name ?a
              :in $ ?age
              :where  [ ?e :person/name ?name ] 
                      [ ?e :person/age ?a ]
                      [ (<= ?a ?age) ]
            ]
          """), database, DLong(40)).map{
            case (id: DLong, name: DString, age: DLong) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
            case e => throw new RuntimeException("unexpected result")
          }

          val l2 = Datomic.q(Query.manual[Args2, Args3]("""
            [ 
              :find ?e ?name ?a
              :in $ ?age
              :where  [ ?e :person/name ?name ] 
                      [ ?e :person/age ?a ]
                      [ (not= ?a ?age) ]
            ]
          """), database, DLong(35L)).map{
            case (id: DLong, name: DString, age: DLong) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
            case e => throw new RuntimeException("unexpected result")
          }

          val l3 = Datomic.q(Query.manual[Args2, Args3]("""
            [ 
              :find ?e ?name ?a
              :in $ ?age
              :where  [ ?e :person/name ?name ] 
                      [ ?e :person/age ?a ]
                      [ (== ?a ?age) ]
            ]
          """), database, DLong(35L)).map{
            case (id: DLong, name: DString, age: DLong) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
            case e => throw new RuntimeException("unexpected result")
          }

          (l1, l2, l3)
        }
      }.recover{
        case e => failure(e.getMessage)
      }

      val (a, b, c) = Await.result(
        fut,
        Duration("30 seconds")
      ) 

      (a.toSet, b.toSet, c.toSet) must beEqualTo((
        Set(DString("toto") -> DLong(30L), DString("tutu") -> DLong(35L)),
        Set(DString("toto") -> DLong(30L), DString("tata") -> DLong(54L)),
        Set(DString("tutu") -> DLong(35L))
      ))
    }
  }
}