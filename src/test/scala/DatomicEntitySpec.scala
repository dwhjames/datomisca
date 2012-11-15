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
import DatomicData._
import EntityImplicits._

@RunWith(classOf[JUnitRunner])
class DatomicEntitySpec extends Specification {
  "Datomic" should {
    "create entity" in {
      implicit val uri = "datomic:mem://datomicschemaspec"

      import scala.concurrent.ExecutionContext.Implicits.global

      case class Person(name: String, age: Int)

      object PersonSchema {
        val name = Attribute( KW(":person/name"), SchemaType.string, Cardinality.one).withDoc("Person's name")
        val age = Attribute( KW(":person/age"), SchemaType.long, Cardinality.one).withDoc("Person's name")

        val schema = Seq(name, age)
      }

      val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Int]
      )(Person)

      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }      

      Await.result(
        transact(PersonSchema.schema).map { tx =>
          println("TX:"+tx)
          transact(
            addEntity(DId(Partition.USER))(
              person / "name" -> "toto",
              person / "age" -> 30
            ),
            addEntity(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age" -> 54
            ),
            addEntity(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age" -> 23
            )
          ).map{ tx => 
            println("Provisioned data... TX:%s".format(tx))

            query[Args0, Args1]("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """).one().execute().map{
              case e: DLong =>
                val entity = database.entity(e)
                fromEntity(entity)(personReader).map {
                  case Person(name, age) => println(s"Found person with name $name and age $age")
                }
              case _ => failure("error")
            }.recover{
              case e => failure(e.getMessage)
            }
          }.recover{
            case e => failure(e.getMessage)
          }
        },
        Duration("2 seconds")
      )

      success
    }
  }
}