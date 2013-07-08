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

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DatomicCompilerSpec extends Specification {
  "Datomic" should {
    "query simple" in {
      val uri = "datomic:mem://DatomicCompilerSpec"
      Await.result(
      DatomicBootstrap(uri) map { tx =>
        
        implicit val conn = Datomic.connect(uri)

        val person = Namespace("person")
  
        val query = Query("""
          [ :find ?e ?name
            :in $ ?age
            :where  [ ?e :person/name ?name ] 
                    [ ?e :person/age ?age ]
                    [ ?e :person/character :person.character/violent ]
          ]
        """)

        val qf = Datomic.q(query, Datomic.database, DLong(54L)) map {
          case (DLong(e), DString(n)) => 
            val entity = Datomic.database.entity(e)
            println(s"Q2 entity: $e name: $n - e: ${entity.get(person / "character")}")
            n must beEqualTo("tutu")
        }
        
        Datomic.q(
          Query("""
            [ :find ?e ?name ?age
              :in $ ?age
              :where  [ ?e :person/name ?name ] 
                      [ ?e :person/age ?a ]
                      [ (< ?a ?age) ]
            ]
          """
        ), Datomic.database, DLong(30)) map {
          case (DLong(entity), DString(name), DLong(age)) => 
            println(s"""Q3 entity: $entity - name: $name - age: $age""")
            name must beEqualTo("tata")
        }

        success
      },
      Duration("3 seconds")
    )

    Datomic.deleteDatabase(uri)
    }
  }
}
