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

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


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
      
      println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")

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
        Datomic.KW(":person/name")          -> "toto",
        Datomic.KW(":person/age")           -> 30L,
        Datomic.KW(""":person/character""") -> Seq(weak, dumb)
      ).toString must beEqualTo(
        AddEntity(id)(
          Keyword(person, "name")      -> DString("toto"),
          Keyword(person, "age")       -> DLong(30L),
          Keyword(person, "character") -> DColl(weak.ref, dumb.ref)
        ).toString
      )

      Entity.addRaw("""{
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
        Entity.addRaw("""{
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
