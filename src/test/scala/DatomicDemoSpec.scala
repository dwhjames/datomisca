import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
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
        println(s"Provisioned schema... TX: $tx")

        /* AddEntity different syntaxes from most programmatic to macrocompiled using inline variables 
         * POTENTIAL DEMO :
         *  - remove a ] from addEntity to show compiling error
         */
        conn.transact(
          AddEntity(DId(Partition.USER))(
            person / "name" -> DString("toto"),
            person / "age" -> DLong(30L),
            person / "character" -> DSet(weak.ref, dumb.ref)
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
        ) map { tx => 
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
            case (DLong(id), DString(name), DLong(age)) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
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
            case (DLong(id), DString(name), DLong(age)) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
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
            case (DLong(id), DString(name), DLong(age)) => 
              // can get entity there
              val entity = database.entity(id)
              println(s"""entity: $id - name $name - characters ${entity.get(person/"character")}""")
              name -> age
          }

          (l1, l2, l3)
        }
      }

      val (a, b, c) = Await.result(
        fut,
        Duration("30 seconds")
      ) 

      (a.toSet, b.toSet, c.toSet) must beEqualTo((
        Set("toto" -> 30L, "tutu" -> 35L),
        Set("toto" -> 30L, "tata" -> 54L),
        Set("tutu" -> 35L)
      ))
    }
  }
}
