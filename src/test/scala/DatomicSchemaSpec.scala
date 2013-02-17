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
import java.util.concurrent.TimeUnit._

import datomisca._
import scala.concurrent.ExecutionContext.Implicits.global

import Datomic._

@RunWith(classOf[JUnitRunner])
class DatomicSchemaSpec extends Specification {
  "Datomic" should {
    "create simple schema and provision data" in {

      val uri = "datomic:mem://datomicschemaspec"

      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, Datomic.createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val violent = AddIdent(Keyword(person.character, "violent"))
      val weak = AddIdent(Keyword(person.character, "weak"))
      val clever = AddIdent(Keyword(person.character, "clever"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val schema = Seq(
        Attribute( Keyword(Namespace("person"), "name"), SchemaType.string, Cardinality.one).withDoc("Person's name"),
        Attribute( Keyword(Namespace("person"), "age"), SchemaType.long, Cardinality.one).withDoc("Person's age"),
        Attribute( Keyword(Namespace("person"), "character"), SchemaType.ref, Cardinality.many).withDoc("Person's characterS"),
        violent,
        weak,
        clever,
        dumb
      )

      implicit val conn = Datomic.connect(uri)

      Datomic.transact(schema).map{ tx => 
        println("Provisioned schema... TX:%s".format(tx))

        Datomic.transact(
          AddEntity(DId(Partition.USER))(
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
        ).map{ tx => 
          println("Provisioned data... TX:%s".format(tx))
        }.recover{
          case e => println(e.getMessage)
        }

        Datomic.q(Query.pure("""
          [ :find ?e
            :where [ ?e :person/name "toto" ] 
          ]
        """)).map {
          case List(DLong(totoId)) => 
            Datomic.transact(
              Entity.retract(totoId)
            ).map{ tx => 
              println("Retracted data... TX:%s".format(tx))

              Datomic.q(Query.pure("""
                [ :find ?e
                  :where  [ ?e :person/name "toto" ] 
                ]
              """)).isEmpty must beTrue
            }.recover{
              case e => println(e.getMessage)
            }
        }
      }.recover{
        case e => println(e.getMessage)
      }

      success
    }
  }
}