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
class SchemaManagerSpec extends Specification {

  val schemaTag = Datomic.KW(":my-schema-tag")

  case object SchemaA {
    val attrA = Attribute(Datomic.KW(":attrA"), SchemaType.string, Cardinality.one)

    val name     = "SchemaA"
    val requires = Seq.empty[String]
    val txData   = Seq(attrA)
  }
  case object SchemaB {
    val attrB = Attribute(Datomic.KW(":attrB"), SchemaType.string, Cardinality.one)

    val name     = "SchemaB"
    val requires = Seq(SchemaA.name)
    val txData   = Seq(attrB)
  }

  case object SchemaC {
    val attrC = Attribute(Datomic.KW(":attrC"), SchemaType.string, Cardinality.one)

    val name     = "SchemaC"
    val requires = Seq(SchemaB.name)
    val txData   = Seq(attrC)
  }

  val schemaMap = Map(
      SchemaA.name ->
        (SchemaA.requires,
          Seq(SchemaA.txData)),
      SchemaB.name ->
        (SchemaB.requires,
          Seq(SchemaB.txData)),
      SchemaC.name ->
        (SchemaC.requires,
          Seq(SchemaC.txData))
    )

  "Schema Manager" should {

    val uri = "datomic:mem://SchemaManagerSpec"
    println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")
    implicit val conn = Datomic.connect(uri)

    "provision schemas if needed" in {

      Await.result(
        for {
          _ <- SchemaManager.installSchema(schemaTag, schemaMap, SchemaA.name)
        } yield {
          implicit val db = conn.database

          SchemaManager.hasAttribute(SchemaA.attrA.ident) must beTrue
          SchemaManager.hasAttribute(SchemaB.attrB.ident) must beFalse

          SchemaManager.hasSchema(schemaTag, SchemaA.name) must beTrue
          SchemaManager.hasSchema(schemaTag, SchemaB.name) must beFalse
        },
        Duration("10 seconds")
      )


      Await.result(
        for {
          _ <- SchemaManager.installSchema(schemaTag, schemaMap, SchemaC.name)
        } yield {
          implicit val db = conn.database

          SchemaManager.hasAttribute(SchemaB.attrB.ident) must beTrue
          SchemaManager.hasAttribute(SchemaC.attrC.ident) must beTrue

          SchemaManager.hasSchema(schemaTag, SchemaB.name) must beTrue
          SchemaManager.hasSchema(schemaTag, SchemaC.name) must beTrue
        },
        Duration("10 seconds")
      )

      success
    }
  }

}
