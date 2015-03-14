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

import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global


class SchemaFactSchemaEntitySpec
  extends FlatSpec
     with Matchers
     with OptionValues
     with ScalaFutures
     with DatomicFixture
{

  "SchemaFact" should "add a new value fact" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val totoId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "toto").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        SchemaFact.add(totoId)(PersonSampleData.Schema.ageAttr -> 31L)
      )
    ) { txReport =>
      txReport.dbBefore.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (PersonSampleData.toto.age)
      txReport.dbAfter.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (31L)
    }
  }


  it should "add a new reference fact" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val tutuId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "tutu").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        SchemaFact.add(tutuId)(PersonSampleData.Schema.moodAttr -> PersonSampleData.Schema.angryMood)
      )
    ) { txReport =>
      txReport.dbBefore.entity(tutuId).read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should not contain (PersonSampleData.Schema.angryMood.ident)
      txReport.dbAfter.entity(tutuId).read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should contain (PersonSampleData.Schema.angryMood.ident)
    }
  }


  it should "retract a value fact" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val totoId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "toto").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        SchemaFact.retract(totoId)(PersonSampleData.Schema.ageAttr -> PersonSampleData.toto.age)
      )
    ) { txReport =>
      txReport.dbBefore.entity(totoId).keySet should contain (PersonSampleData.Schema.ageAttr.toString)
      txReport.dbAfter.entity(totoId).keySet should not contain (PersonSampleData.Schema.ageAttr.toString)
    }
  }


  it should "retract a reference fact" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val tutuId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "tutu").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        SchemaFact.retract(tutuId)(PersonSampleData.Schema.moodAttr -> PersonSampleData.Schema.stressedMood)
      )
    ) { txReport =>
      txReport.dbBefore.entity(tutuId).read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should contain (PersonSampleData.Schema.stressedMood.ident)
      txReport.dbAfter.entity(tutuId).read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should not contain (PersonSampleData.Schema.stressedMood.ident)
    }
  }



  "SchemaEntity" should "add a new fact" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val totoId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "toto").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        (SchemaEntity.newBuilder += (PersonSampleData.Schema.ageAttr -> 31L)) withId totoId
      )
    ) { txReport =>
      txReport.dbBefore.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (PersonSampleData.toto.age)
      txReport.dbAfter.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (31L)
    }
  }


  it should "add a new value fact (with a Some of Option)" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val totoId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "toto").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        (SchemaEntity.newBuilder +?= (PersonSampleData.Schema.ageAttr -> Some(31L))) withId totoId
      )
    ) { txReport =>
      txReport.dbBefore.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (PersonSampleData.toto.age)
      txReport.dbAfter.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (31L)
    }
  }


  it should "add multiple facts" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val tutuId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "tutu").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        (SchemaEntity.newBuilder
          ++= (PersonSampleData.Schema.moodAttr -> Set(PersonSampleData.Schema.angryMood, PersonSampleData.Schema.excitedMood))
        ) withId tutuId
      )
    ) { txReport =>
      txReport.dbBefore
              .entity(tutuId)
              .read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should contain only (
        PersonSampleData.Schema.sadMood.ident,
        PersonSampleData.Schema.stressedMood.ident
      )

      txReport.dbAfter
              .entity(tutuId)
              .read[Set[Keyword]](PersonSampleData.Schema.moodAttr) should contain allOf (
        PersonSampleData.Schema.sadMood.ident,
        PersonSampleData.Schema.stressedMood.ident,
        PersonSampleData.Schema.angryMood.ident,
        PersonSampleData.Schema.excitedMood.ident
      )
    }
  }


  it should "add a from a partial entity" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    val totoId = Datomic.q(PersonSampleData.queryPersonIdByName, conn.database(), "toto").head.asInstanceOf[Long]

    whenReady(
      Datomic.transact(
        (SchemaEntity.newBuilder
          ++= (SchemaEntity.newBuilder
                 += (PersonSampleData.Schema.ageAttr -> 31L)
              ).partial()
        ) withId totoId
      )
    ) { txReport =>
      txReport.dbBefore.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (PersonSampleData.toto.age)
      txReport.dbAfter.entity(totoId).apply(PersonSampleData.Schema.ageAttr) should be (31L)
    }
  }

}
