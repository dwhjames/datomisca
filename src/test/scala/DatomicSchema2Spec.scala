import scala.language.reflectiveCalls

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

import datomisca._
import Datomic._

@RunWith(classOf[JUnitRunner])
class DatomicSchema2Spec extends Specification {
  "Datomic" should {
    "create simple schema and provision data" in {
      import scala.concurrent.ExecutionContext.Implicits.global

      val uri = "datomic:mem://DatomicSchema2Spec"

      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))

      implicit val conn = Datomic.connect(uri)

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val violent = AddIdent(person.character / "violent")
      val weak = AddIdent(Keyword(person.character, "weak"))
      val clever = AddIdent(Keyword(person.character, "clever"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val schema = Seq(
        Attribute( KW(":person/name"), SchemaType.string, Cardinality.one).withDoc("Person's name"),
        Attribute( KW(":person/age"), SchemaType.long, Cardinality.one).withDoc("Person's age"),
        Attribute( KW(":person/character"), SchemaType.ref, Cardinality.many).withDoc("Person's characters"),
        violent,
        weak,
        clever,
        dumb
      )

      Await.result(Datomic.transact(schema).flatMap{ tx => 
        println("Provisioned schema... TX:%s".format(tx))

        val id = DId(Partition.USER)
        Datomic.transact(
          AddEntity(id)(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(weak.ref, dumb.ref)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tutu"),
            Keyword(person, "age") -> DLong(54L),
            Keyword(person, "character") -> DSet(violent.ref, clever.ref)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tata"),
            Keyword(person, "age") -> DLong(23L),
            Keyword(person, "character") -> DSet(weak.ref, clever.ref)
          )
        ).flatMap{ tx => 
          println("Provisioned data... TX:%s".format(tx))
          val totoId = Datomic.q(Query.pure("""
          [ :find ?e
            :where [ ?e :person/name "toto" ] 
          ]
          """)).head.head.asInstanceOf[DLong]
          //.map {
          //  case List(totoId: DLong) => 
          println("TOTO:"+totoId)
          Datomic.transact(
            RetractEntity(totoId)
          ).flatMap{ tx => 
            println("Retracted data... TX:%s".format(tx))

            Datomic.q(Query.pure("""
              [ :find ?e
                :where  [ ?e :person/name "toto" ] 
              ]
            """)).isEmpty must beTrue

            println("Provisioned data... TX:%s".format(tx))
            val tutuId = Datomic.q(Query.pure("""
            [ :find ?e
              :where [ ?e :person/name "tutu" ] 
            ]
            """)).head.head.asInstanceOf[DLong]
            //.map {
            //  case List(totoId: DLong) => 
            println("TUTU:"+tutuId)
            Datomic.transact(
              RetractEntity(tutuId)
            ).map{ tx => 
              println("Retracted data... TX:%s".format(tx))

              Datomic.q(Query.pure("""
                [ :find ?e
                  :where  [ ?e :person/name "tutu" ] 
                ]
              """)).isEmpty must beTrue
            }
            //}
                  
          }
          //}
        }        
      }.recover{
        case e => failure(e.getMessage)
      },
      Duration("30 seconds"))
    }
  }
}