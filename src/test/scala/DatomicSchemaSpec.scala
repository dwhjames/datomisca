import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._

import datomisca._
import Datomic._

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicSchemaSpec extends Specification {
  "Datomic" should {
    "create simple schema and provision data" in {

      val uri = "datomic:mem://datomicschemaspec"

      //DatomicBootstrap(uri)
      println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val violent = AddIdent(person.character / "violent")
      val weak    = AddIdent(person.character / "weak")
      val clever  = AddIdent(person.character / "clever")
      val dumb    = AddIdent(person.character / "dumb")

      val schema = Seq(
        Attribute(person / "name",      SchemaType.string, Cardinality.one) .withDoc("Person's name"),
        Attribute(person / "age",       SchemaType.long,   Cardinality.one) .withDoc("Person's age"),
        Attribute(person / "character", SchemaType.ref,    Cardinality.many).withDoc("Person's characters"),
        violent,
        weak,
        clever,
        dumb
      )

      implicit val conn = Datomic.connect(uri)

      Datomic.transact(schema) map { tx => 
        println(s"Provisioned schema... TX: $tx")

        Datomic.transact(
          Entity.add(DId(Partition.USER))(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(weak, dumb)
          ),
          Entity.add(DId(Partition.USER))(
            person / "name"      -> "tutu",
            person / "age"       -> 54L,
            person / "character" -> Set(violent, clever)
          ),
          Entity.add(DId(Partition.USER))(
            person / "name"      -> "tata",
            person / "age"       -> 23L,
            person / "character" -> Set(weak, clever)
          )
        ) map { tx => 
          println(s"Provisioned data... TX: $tx")
        }

        Datomic.q(Query.pure("""
          [ :find ?e
            :where [ ?e :person/name "toto" ] 
          ]
        """), database) map {
          case List(DLong(totoId)) => 
            Datomic.transact(
              Entity.retract(totoId)
            ) map { tx => 
              println("Retracted data... TX:%s".format(tx))

              Datomic.q(Query.pure("""
                [ :find ?e
                  :where  [ ?e :person/name "toto" ] 
                ]
              """), database).isEmpty must beTrue
            }
        }
      }

      success
    }
  }
}
