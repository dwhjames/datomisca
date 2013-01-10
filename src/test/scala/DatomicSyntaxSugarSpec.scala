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
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

import reactivedatomic._
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
      
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(Keyword(person.character, "weak"))
      val dumb = AddIdent(Keyword(person.character, "dumb"))

      val id = DId(Partition.USER)

      implicit val conn = Datomic.connect(uri)

      Datomic.addToEntity(id)(
        person / "name" -> "toto",
        person / "age" -> 30L,
        person / "character" -> Seq(weak, dumb)
      ).toString must beEqualTo(
        AddToEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
        ).toString
      )

      Datomic.addToEntity(id)(
        KW(":person/name") -> "toto",
        KW(":person/age") -> 30L,
        KW(""":person/character""") -> Seq(weak, dumb)
      ).toString must beEqualTo(
        AddToEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
        ).toString
      )

      Datomic.addToEntity("""{
        :db/id $id
        :person/name "toto"
        :person/age 30
        :person/character [ $weak $dumb ]
      }""").toString must beEqualTo(
        AddToEntity(id)(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
        ).toString
      )

      Datomic.transact(
        Datomic.addToEntity("""{
          :db/id ${DId(Partition.USER)}
          :person/name "toto"
          :person/age 30
          :person/character [ $weak $dumb ]
        }""")
      ).map{ tx =>
        println("Provisioned data... TX:%s".format(tx))
      }.recover{
        case e => println(e.getMessage)
      }

      success
    }
  }
}