import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._

class DatomicParserOpsSpec extends Specification {
  sequential

  import scala.concurrent.ExecutionContext.Implicits.global

  val uri = "datomic:mem://DatomicParserOpsSpec"

  def startDB = {
    println(s"created DB with uri $uri: ${createDatabase(uri)}")
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
        List(
          Fact.add(ops(0).fact.id)(KW(":db/ident") -> DRef(KW(":region/n")))
        ).toString
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
        List(
          Fact.add(id)(KW(":db/ident") -> DRef(KW(":region/n")))
        ).toString
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
          Fact.add(ops(0).fact.id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Fact.add(id)(KW(":db/ident") -> DRef(KW(":region/n")))
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
        List(
          Fact.retract(ops(0).fact.id)(KW(":db/ident") -> DRef(KW(":region/n")))
        ).toString
      )
    }

    "5 - map simple retractEntity op" in {
      implicit val conn = Datomic.connect(uri)  

      val ops = Datomic.ops("""[
        [:db/retractEntity 1234]
      ]""")

      println(s"4 - Ops:$ops")

      ops.toString must beEqualTo(
        List(
          Entity.retract(1234L)
        ).toString
      )
    }

    "6 - map simple AddEntity op" in {
      implicit val conn = Datomic.connect(uri)  

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(person.character / "weak")
      val dumb = AddIdent(person.character / "dumb")
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
          Entity.add(id)(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(weak.ref, dumb.ref)
          )
        ).toString
      )
    }

    "7 - map mix ops" in {
      implicit val conn = Datomic.connect(uri)  

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      val weak = AddIdent(person.character / "weak")
      val dumb = AddIdent(person.character / "dumb")

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
          Fact.add(ops(0).asInstanceOf[AddFact].fact.id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Fact.add(id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Fact.retract(ops(2).asInstanceOf[RetractFact].fact.id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Entity.retract(1234L),
          Entity.add(id)(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(weak.ref, dumb.ref)
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
          Fact.add(ops(0).asInstanceOf[AddFact].fact.id)(KW(":db/ident") -> DRef(KW(":character/weak"))),
          Fact.add(ops(1).asInstanceOf[AddFact].fact.id)(KW(":db/ident") -> DRef(KW(":character/dumb"))),
          Fact.add(ops(2).asInstanceOf[AddFact].fact.id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Fact.retract(ops(3).asInstanceOf[RetractFact].fact.id)(KW(":db/ident") -> DRef(KW(":region/n"))),
          Entity.retract(1234L),
          Entity.add(ops(5).asInstanceOf[AddEntity].id)(
            person / "name"      -> "toto, tata",
            person / "age"       -> 30L,
            person / "character" -> Set(DRef(KW(":character/_weak")), DRef(KW(":character/dumb-toto")))
          ),
          Entity.add(ops(6).asInstanceOf[AddEntity].id)(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(DRef(KW(":character/_weak")), DRef(KW(":character/dumb-toto")))
          )
        ).toString
      )
    }

  }

}
