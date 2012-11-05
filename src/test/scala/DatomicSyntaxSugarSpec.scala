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
class DatomicSyntaxSugarSpec extends Specification {
  "Datomic" should {
    "accept syntactic sugar" in {
      import Datomic._
      import DatomicData._

      implicit val uri = "datomic:mem://datomicschemaspec"

      DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(Keyword(person.character, "weak"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val id = DId(Partition.USER)

      Datomic.addEntity(id)(
        person / "name" -> "toto",
        person / "age" -> 30L,
        person / "character" -> Seq(weak, dumb)
      ) must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSeq(weak.ident, dumb.ident)
        )
      )

      Datomic.addEntity(id)(
        KW(":person/name") -> "toto",
        KW(":person/age") -> 30L,
        KW(""":person/character""") -> Seq(weak, dumb)
      ) must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSeq(weak.ident, dumb.ident)
        )
      )

      Datomic.addEntity("""{
        :db/id $id
        :person/name "toto"
        :person/age 30
        :person/character [ $weak $dumb ]
      }""") must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSeq(weak.ident, dumb.ident)
        )
      )

      Datomic.transact("""{
        :db/id ${DId(Partition.USER)}
        :person/name "toto"
        :person/age 30
        :person/character [ $weak $dumb ]
      }""").map{ tx =>
        println("Provisioned data... TX:%s".format(tx))
      }.recover{
        case e => println(e.getMessage)
      }

      success
    }
  }
}