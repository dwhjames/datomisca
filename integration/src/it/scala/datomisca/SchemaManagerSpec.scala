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

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures


class SchemaManagerSpec
  extends FlatSpec
     with Matchers
     with ScalaFutures
     with DatomicFixture
{

  val schemaTag = Datomic.KW(":my-schema-tag")

  case object SchemaA {
    var attrA = Attribute(Datomic.KW(":attrA"), SchemaType.string, Cardinality.one)

    var name     = "SchemaA"
    val requires = Seq.empty[String]
    def txData   = Seq(attrA)
  }
  case object SchemaB {
    val attrB = Attribute(Datomic.KW(":attrB"), SchemaType.string, Cardinality.one)

    val name     = "SchemaB"
    def requires = Seq(SchemaA.name)
    def txData   = Seq(attrB)
  }

  case object SchemaC {
    val attrC = Attribute(Datomic.KW(":attrC"), SchemaType.string, Cardinality.one)

    val name     = "SchemaC"
    def requires = Seq(SchemaB.name)
    def txData   = Seq(attrC)
  }

  def schemaMap = Map(
      SchemaA.name ->
        (SchemaA.requires ->
          Seq(SchemaA.txData)),
      SchemaB.name ->
        (SchemaB.requires ->
          Seq(SchemaB.txData)),
      SchemaC.name ->
        (SchemaC.requires ->
          Seq(SchemaC.txData))
    )

  "Schema Manager" should "provision schemas if needed" in withDatomicDB { implicit conn =>

    // Install schema A into an empty db
    whenReady(SchemaManager.installSchema(schemaTag, schemaMap, SchemaA.name)) { changed =>
      implicit val db = conn.database()

      changed should be (true)

      SchemaManager.hasAttribute(SchemaA.attrA.ident) should be (true)
      SchemaManager.hasAttribute(SchemaB.attrB.ident) should be (false)

      SchemaManager.hasSchema(schemaTag, SchemaA.name) should be (true)
      SchemaManager.hasSchema(schemaTag, SchemaB.name) should be (false)
    }

    // reïnstall schema A
    whenReady(SchemaManager.installSchema(schemaTag, schemaMap, SchemaA.name)) { changed =>
      changed should be (false)
    }

    // install schema C
    whenReady(SchemaManager.installSchema(schemaTag, schemaMap, SchemaC.name)) { changed =>
      implicit val db = conn.database()

      changed should be (true)

      SchemaManager.hasAttribute(SchemaB.attrB.ident) should be (true)
      SchemaManager.hasAttribute(SchemaC.attrC.ident) should be (true)

      SchemaManager.hasSchema(schemaTag, SchemaB.name) should be (true)
      SchemaManager.hasSchema(schemaTag, SchemaC.name) should be (true)
    }

    // install schema B (which should be a reïnstall)
    whenReady(SchemaManager.installSchema(schemaTag, schemaMap, SchemaB.name)) { changed =>
      changed should be (false)
    }

    // mutate the version name of schema A (simulating an update)
    val schemaANameOrig = SchemaA.name
    SchemaA.name = "SchemaA_v2"

    val schemaAAttrOrig = SchemaA.attrA.ident
    SchemaA.attrA = Attribute(Datomic.KW(":attrA-v2"), SchemaType.string, Cardinality.one)

    // reïnstall schema C (which should be an update of schema A)
    whenReady(SchemaManager.installSchema(schemaTag, schemaMap, SchemaC.name)) { changed =>
      implicit val db = conn.database()

      changed should be (true)

      SchemaManager.hasAttribute(SchemaA.attrA.ident) should be (true)
      SchemaManager.hasAttribute(SchemaB.attrB.ident) should be (true)
      SchemaManager.hasAttribute(SchemaC.attrC.ident) should be (true)


      SchemaManager.hasSchema(schemaTag, SchemaA.name) should be (true)
      SchemaManager.hasSchema(schemaTag, SchemaB.name) should be (true)
      SchemaManager.hasSchema(schemaTag, SchemaC.name) should be (true)

      // original is still there
      SchemaManager.hasAttribute(schemaAAttrOrig) should be (true)
      SchemaManager.hasSchema(schemaTag, schemaANameOrig) should be (true)
    }

  }
}
