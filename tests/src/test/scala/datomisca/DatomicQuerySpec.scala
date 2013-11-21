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
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


@RunWith(classOf[JUnitRunner])
class DatomicQuerySpec extends Specification {
  sequential
  val uri = "datomic:mem://datomicqueryspec"
  val person = Namespace("person")

  def startDB = {
    println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")

    implicit val conn = Datomic.connect(uri)

    Await.result(
      DatomicBootstrap(uri),
      Duration("3 seconds")
    )
  }

  def stopDB = {
    Datomic.deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic" should {
    "1 - pure query" in {

      implicit val conn = Datomic.connect(uri)


      Datomic.q(Query("""
        [ :find ?e ?n
          :where  [ ?e :person/name ?n ]
                  [ ?e :person/character :person.character/violent ]
        ]
      """), Datomic.database) map {
        case (e: Long, n: String) =>
          val entity = Datomic.database.entity(e)
          println(s"1 - entity: $e name: $n - e: ${entity.get(person / "character")}")
      }

      success
    }

    "2 - typed query with rule with 2 params only" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query("""
        [:find ?e :where [?e :person/name]]
      """)
      Datomic.q(q, Datomic.database) map {
        case e: Long =>
          val entity = Datomic.database.entity(e)
          println(s"2 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
      }

      success
    }

    "3 - typed query with rule with params variable length" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query("""
        [
         :find ?e
         :in $ [?names ...]
         :where [?e :person/name ?names]
        ]
      """), Datomic.database, Seq("toto", "tata")) map {
        case e: Long =>
          val entity = Datomic.database.entity(e)
          println(s"3 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
      }

      success
    }

    "4 - typed query with rule with list of tuple inputs" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query("""
        [
         :find ?e ?name ?age
         :in $ [[?name ?age]]
         :where [?e :person/name ?name]
                [?e :person/age ?age]
        ]
      """)
      Datomic.q(
        q, Datomic.database,
        Datomic.list(
          Datomic.list("toto", 30L),
          Datomic.list("tutu", 54L)
        )
      ) map {
        case (e: Long, n: String, a: Long) =>
          println(s"4 - entity: $e name: $n - age: $a")
      }

      success
    }

    "5 - typed query with fulltext query" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query("""
        [
         :find ?e ?n
         :where [(fulltext $ :person/name "toto") [[ ?e ?n ]]]
        ]
      """)
      Datomic.q(q, Datomic.database) map {
        case (e: Long, n: String) =>
          println(s"5 - entity: $e name: $n")
      }

      success
    }

    "8 - query with rule alias" in {
      implicit val conn = Datomic.connect(uri)

      val totoRule = Query.rules("""
        [ [ [toto ?e]
           [?e :person/name "toto"]
        ] ]
      """)

      val q = Query("""
        [
          :find ?e ?age
          :in $ %
          :where [?e :person/age ?age]
                 (toto ?e)
        ]
      """)

      Datomic.q(q, Datomic.database, totoRule) map {
        case (e: Long, age: Long) =>
          println(s"e: $e - age: $age")
          age must beEqualTo(30L)
      }

      success
    }

    "9 - query with with" in {
      implicit val conn = Datomic.connect(uri)

      val q = Query("""
        [ :find ?e ?n
          :with ?age
          :where  [ ?e :person/name ?n ]
                  [ ?e :person/age ?age ]
                  [ ?e :person/character :person.character/violent ]
        ]
      """)

      Datomic.q(q, Datomic.database) map {
        case (e: Long, name: String) =>
          println(s"e: $e - name: $name")
          name must beEqualTo("tutu")
      }

      success
    }

    "12 - passing database when no :in" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query("""
        [:find ?e :where [?e :person/name]]
      """)
      Datomic.q(q, Datomic.database) map {
        case e: Long =>
          val entity = Datomic.database.entity(e)
          println(s"12 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
      }

      success
    }

    "13 - passing datasource when no :in" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query("""
        [:find ?firstname ?lastname :where [?firstname ?lastname]]
      """)
      Datomic.q(q, Seq(Seq("John", "Smith"))) map {
        case (firstname: String, lastname: String) =>
          firstname must beEqualTo("John")
          lastname must beEqualTo("Smith")
          println(s"13 - firstname: $firstname lastname: $lastname")
      }

      success
    }

    "14 - passing datasource with :in" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query("""
        [:find ?firstname ?lastname :in $ :where [?firstname ?lastname]]
      """)
      Datomic.q(q, Seq(Seq("John", "Smith"))) map {
        case (firstname: String, lastname: String) =>
          firstname must beEqualTo("John")
          lastname must beEqualTo("Smith")
          println(s"14 - firstname: $firstname lastname: $lastname")
      }

      success
    }
  }
}
