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

        Datomic.q(Query("""
          [ :find ?e
            :where [ ?e :person/name "toto" ] 
          ]
        """), Datomic.database) map {
          case totoId: Long => 
            Datomic.transact(
              Entity.retract(totoId)
            ) map { tx => 
              println("Retracted data... TX:%s".format(tx))

              Datomic.q(Query("""
                [ :find ?e
                  :where  [ ?e :person/name "toto" ] 
                ]
              """), Datomic.database).isEmpty must beTrue
            }
        }
      }

      success
    }
  }
}
