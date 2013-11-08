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
class DatomicTransactSpec extends Specification {
  "Datomic" should {
    "operation simple" in {

      val uri = "datomic:mem://DatomicTransactSpec"

      Await.result(
        DatomicBootstrap(uri),
        Duration("3 seconds")
      )

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      //val violent = Enum(Keyword("violent", person.character)) //":person.character/violent"
      //val weak = Enum(Keyword("weak", person.character))

      //val toto = Add( Fact(Id(Partition.USER), Keyword("ident", Namespace.DB), DString(":person/toto")) )
      //val toto = Add( Id(Partition.USER), Keyword("ident", Namespace.DB), DString(":person/toto") )
      val violent = AddIdent(person.character / "violent")
      val weak    = AddIdent(person.character / "weak", Partition.USER)
      
      val person1 = Entity.add( DId(Partition.USER) )(
        person / "name"      -> "bob",
        person / "age"       -> 30L,
        person / "character" -> Set(violent, weak)
      )

      implicit val conn = Datomic.connect(uri)

      Datomic.transact(Seq(
        violent,
        weak,
        person1
      )) map { tx => 
        println(s"Provisioned data... TX: $tx")
      }

      //println("DID:"+DId(Partition.USER).value.getClass)
      Datomic.q(Query("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """), Datomic.database).map {
        case (DLong(e), DString(n)) => 
        println(s"PART ${datomic.Peer.part(e.underlying).getClass}")
        val entity = Datomic.database.entity(e)
        println(s"Q2 entity: $e name: $n - e: ${entity.get(person / "character")}")
      }

      println("Attribute:"+Attribute( 
        person / "name",
        SchemaType.string,
        Cardinality.one
      ).withDoc("Person's name"))

      success
    }
  }
}
