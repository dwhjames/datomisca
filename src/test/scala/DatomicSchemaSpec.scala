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

import reactivedatomic._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicSchemaSpec extends Specification {
  "Datomic" should {
    "create simple schema and provision data" in {
      import Datomic._
      import DatomicData._

      implicit val uri = "datomic:mem://datomicschemaspec"

      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val violent = AddIdent(Keyword(person.character, "violent"))
      val weak = AddIdent(Keyword(person.character, "weak"))
      val clever = AddIdent(Keyword(person.character, "clever"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val schema = Schema(
        Field( Keyword(Namespace("person"), "name"), SchemaType.string, Cardinality.one).withDoc("Person's name"),
        Field( Keyword(Namespace("person"), "age"), SchemaType.long, Cardinality.one).withDoc("Person's age"),
        Field( Keyword(Namespace("person"), "character"), SchemaType.ref, Cardinality.many).withDoc("Person's characterS"),
        violent,
        weak,
        clever,
        dumb
      )

      connection.provisionSchema(schema).map{ tx => 
        println("Provisioned schema... TX:%s".format(tx))

        connection.transact(
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSeq(weak.ident, dumb.ident)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tutu"),
            Keyword(person, "age") -> DLong(54L),
            Keyword(person, "character") -> DSeq(violent.ident, clever.ident)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tata"),
            Keyword(person, "age") -> DLong(23L),
            Keyword(person, "character") -> DSeq(weak.ident, clever.ident)
          )
        ).map{ tx => 
          println("Provisioned data... TX:%s".format(tx))
        }.recover{
          case e => println(e.getMessage)
        }

        pureQuery("""
          [ :find ?e
            :where [ ?e :person/name "toto" ] 
          ]
        """).prepare().execute().map {
          case List(totoId: DLong) => 
            connection.transact(
              RetractEntity(totoId)
            ).map{ tx => 
              println("Retracted data... TX:%s".format(tx))

              pureQuery("""
                [ :find ?e
                  :where  [ ?e :person/name "toto" ] 
                ]
              """).prepare().execute().isEmpty must beTrue
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