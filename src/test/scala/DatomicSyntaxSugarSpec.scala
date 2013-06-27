import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicSyntaxSugarSpec extends Specification {
  "Datomic" should {
    "accept syntactic sugar" in {
      import Datomic._

      val uri = "datomic:mem://DatomicSyntaxSugarSpec"

      Await.result(
        DatomicBootstrap(uri),
        Duration("3 seconds")
      )
      
      println(s"created DB with uri $uri: ${createDatabase(uri)}")

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(Keyword(person.character, "weak"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val id = DId(Partition.USER)

      implicit val conn = Datomic.connect(uri)

      Entity.add(id)(
        person / "name"      -> "toto",
        person / "age"       -> 30L,
        person / "character" -> Seq(weak, dumb)
      ).toString must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name")      -> DString("toto"),
          Keyword(person, "age")       -> DLong(30L),
          Keyword(person, "character") -> DColl(weak.ref, dumb.ref)
        ).toString
      )

      Entity.add(id)(
        KW(":person/name")          -> "toto",
        KW(":person/age")           -> 30L,
        KW(""":person/character""") -> Seq(weak, dumb)
      ).toString must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name")      -> DString("toto"),
          Keyword(person, "age")       -> DLong(30L),
          Keyword(person, "character") -> DColl(weak.ref, dumb.ref)
        ).toString
      )

      Entity.add("""{
        :db/id $id
        :person/name "toto"
        :person/age 30
        :person/character [ $weak $dumb ]
      }""").toString must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name")      -> DString("toto"),
          Keyword(person, "age")       -> DLong(30L),
          Keyword(person, "character") -> DColl(weak.ref, dumb.ref)
        ).toString
      )

      Datomic.transact(
        Entity.add("""{
          :db/id ${DId(Partition.USER)}
          :person/name "toto"
          :person/age 30
          :person/character [ $weak $dumb ]
        }""")
      ) map { tx =>
        println(s"Provisioned data... TX: $tx")
      }

      success
    }
  }
}
