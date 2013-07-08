/*
 * Copyright 2012 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca

import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


@RunWith(classOf[JUnitRunner])
class DatomicParserOpsSpec extends Specification {
  sequential


  val uri = "datomic:mem://DatomicParserOpsSpec"

  def startDB = {
    println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")
  } 

  def stopDB = {
    Datomic.deleteDatabase(uri)
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
          Fact.add(ops(0).id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n")))
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
          Fact.add(id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n")))
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
          Fact.add(ops(0).id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
          Fact.add(id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n")))
        ).toString
      )
    }

    /*
    FIX
    "4 - map simple retract op" in {
      implicit val conn = Datomic.connect(uri)  

      val ops = Datomic.ops("""[
        [:db/retract #db/id[:db.part/user] :db/ident :region/n]
      ]""")

      println(s"4 - Ops:$ops")

      ops.toString must beEqualTo(
        List(
          Fact.retract(ops(0).id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n")))
        ).toString
      )
    }
    */

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
      // FIX [:db/retract #db/id[:db.part/user] :db/ident :region/n]
      val ops = Datomic.ops("""[
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
        [:db/add $id :db/ident :region/n]
        
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
          Fact.add(ops(0).asInstanceOf[AddFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
          Fact.add(id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
          // FIX Fact.retract(ops(2).asInstanceOf[RetractFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
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
      // FIX [:db/retract #db/id[:db.part/user] :db/ident :region/n]
      val ops = Datomic.parseOps("""
      ;; comment blabla
      [
        [:db/add #db/id[:db.part/user] :db/ident :character/weak]
        ;; comment blabla
        [:db/add #db/id[:db.part/user] :db/ident :character/dumb]
        [:db/add #db/id[:db.part/user] :db/ident :region/n]
        
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
          Fact.add(ops(0).asInstanceOf[AddFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":character/weak"))),
          Fact.add(ops(1).asInstanceOf[AddFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":character/dumb"))),
          Fact.add(ops(2).asInstanceOf[AddFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
          // FIX Fact.retract(ops(3).asInstanceOf[RetractFact].id)(Datomic.KW(":db/ident") -> DRef(Datomic.KW(":region/n"))),
          Entity.retract(1234L),
          Entity.add(ops(4).asInstanceOf[AddEntity].id)(
            person / "name"      -> "toto, tata",
            person / "age"       -> 30L,
            person / "character" -> Set(DRef(Datomic.KW(":character/_weak")), DRef(Datomic.KW(":character/dumb-toto")))
          ),
          Entity.add(ops(5).asInstanceOf[AddEntity].id)(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(DRef(Datomic.KW(":character/_weak")), DRef(Datomic.KW(":character/dumb-toto")))
          )
        ).toString
      )
    }

  }

}
