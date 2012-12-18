import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

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
import scala.util.{Try, Success, Failure}
import java.util.concurrent.TimeUnit._

import reactivedatomic._

import Datomic._
import DatomicData._
import EntityImplicits._

class DatomicParserOpsSpec extends Specification {
  sequential

  import scala.concurrent.ExecutionContext.Implicits.global

  val uri = "datomic:mem://DatomicParserOpsSpec"

  def startDB = {
    println("Creating DB with uri %s: %s".format(uri, createDatabase(uri)))
  } 

  def stopDB = {
    deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic Ops Parsing" should {
    "1 - map simple add op" in {
      implicit val conn = Datomic.connect(uri)  

      val ops = Datomic.ops("""[
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
      ]""")

      println(s"1 - Ops:$ops")

      ops.toString must beEqualTo(
        List(Add(Fact( ops(0).fact.id, KW(":db/ident"), DRef(KW(":region/n"))))).toString
      )
    }

    "2 - map simple add op w/ scala expr" in {
      implicit val conn = Datomic.connect(uri)  

      val id = DId(Partition.USER)

      val ops = Datomic.ops("""[
        [:db/add $id :db/ident :region/n]
      ]""")

      println(s"2 - Ops:$ops")
      ops.toString must beEqualTo(
        List(Add(Fact( id, KW(":db/ident"), DRef(KW(":region/n"))))).toString
      )
    }

    "3 - map 2 adds op w/ scala expr" in {
      implicit val conn = Datomic.connect(uri)  

      val id = DId(Partition.USER)

      val ops = Datomic.ops("""[
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
        [:db/add $id :db/ident :region/n]
      ]""")

      println(s"3 - Ops:$ops")
      ops.toString must beEqualTo(
        List(
          Add(Fact( ops(0).fact.id, KW(":db/ident"), DRef(KW(":region/n")))),
          Add(Fact( id, KW(":db/ident"), DRef(KW(":region/n"))))
        ).toString
      )
    }

    "4 - map simple retract op" in {
      implicit val conn = Datomic.connect(uri)  

      val ops = Datomic.ops("""[
        [:db/retract #db/id[:db.part/user] :db/ident :region/n]
      ]""")

      println(s"4 - Ops:$ops")

      ops.toString must beEqualTo(
        List(Retract(Fact( ops(0).fact.id, KW(":db/ident"), DRef(KW(":region/n"))))).toString
      )
    }

    "5 - map simple retractEntity op" in {
      implicit val conn = Datomic.connect(uri)  

      val ops = Datomic.ops("""[
        [:db/retractEntity 1234]
      ]""")

      println(s"4 - Ops:$ops")

      ops.toString must beEqualTo(
        List(RetractEntity(DLong(1234L))).toString
      )
    }

    "6 - map simple addToEntity op" in {
      implicit val conn = Datomic.connect(uri)  

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(Keyword(Namespace("person.character"), "weak"))
      val dumb = AddIdent(Keyword(Namespace("person.character"), "dumb"))
      val id = DId(Partition.USER)
      val ops = Datomic.ops("""[
        {
          :db/id ${id}
          :person/name "toto"
          :person/age 30
          :person/character [ $weak $dumb ]
        }
      ]""")

      println(s"6 - Ops:$ops")

      ops.toString must beEqualTo(
        List(
          AddToEntity(id)(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
          )
        ).toString
      )
    }

    "7 - map mix ops" in {
      implicit val conn = Datomic.connect(uri)  

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(Keyword(Namespace("person.character"), "weak"))
      val dumb = AddIdent(Keyword(Namespace("person.character"), "dumb"))

      val id = DId(Partition.USER)
      val ops = Datomic.ops("""[
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
        [:db/add $id :db/ident :region/n]
        [:db/retract #db/id[:db.part/user] :db/ident :region/n]
        [:db/retractEntity 1234]
        {
          :db/id ${id}
          :person/name "toto"
          :person/age 30
          :person/character [ $weak $dumb ]
        }
      ]""")

      println(s"7 - Ops:$ops")

      ops.toString must beEqualTo(
        List(
          Add(Fact( ops(0).asInstanceOf[Add].fact.id, KW(":db/ident"), DRef(KW(":region/n")))),
          Add(Fact( id, KW(":db/ident"), DRef(KW(":region/n")))),
          Retract(Fact( ops(2).asInstanceOf[Retract].fact.id, KW(":db/ident"), DRef(KW(":region/n")))),
          RetractEntity(DLong(1234L)),
          AddToEntity(id)(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
          )
        ).toString
      )
    }

    "8 - simple runtime parsing" in {
      implicit val conn = Datomic.connect(uri)  

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val id = DId(Partition.USER)
      val ops = Datomic.parseOps("""
      ;; comment blabla
      [
        [:db/add #db/id[:db.part/user] :db/ident :character/weak]
        ;; comment blabla
        [:db/add #db/id[:db.part/user] :db/ident :character/dumb]
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
        [:db/retract #db/id[:db.part/user] :db/ident :region/n]
        [:db/retractEntity 1234]
        ;; comment blabla
        {
          :db/id #db/id[:db.part/user]
          :person/name "toto, tata"
          :person/age 30
          :person/character [ :character/_weak :character/dumb-toto ]
        }
        { :db/id #db/id[:db.part/user], :person/name "toto",
          :person/age 30, :person/character [ :character/_weak, :character/dumb-toto ]
        }
      ]""").get

      println(s"8 - Ops:$ops")

      ops.toString must beEqualTo(
        List(
          Add(Fact( ops(0).asInstanceOf[Add].fact.id, KW(":db/ident"), DRef(KW(":character/weak")))),
          Add(Fact( ops(1).asInstanceOf[Add].fact.id, KW(":db/ident"), DRef(KW(":character/dumb")))),
          Add(Fact( ops(2).asInstanceOf[Add].fact.id, KW(":db/ident"), DRef(KW(":region/n")))),
          Retract(Fact( ops(3).asInstanceOf[Retract].fact.id, KW(":db/ident"), DRef(KW(":region/n")))),
          RetractEntity(DLong(1234L)),
          AddToEntity(ops(5).asInstanceOf[AddToEntity].id)(
            Keyword(person, "name") -> DString("toto, tata"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(DRef(KW(":character/_weak")), DRef(KW(":character/dumb-toto")))
          ),
          AddToEntity(ops(6).asInstanceOf[AddToEntity].id)(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(DRef(KW(":character/_weak")), DRef(KW(":character/dumb-toto")))
          )
        ).toString
      )
    }

  }

}