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


class RichEntitySpec
  extends FlatSpec
     with Matchers
     with OptionValues
     with DatomicFixture
{

  "RichEntity" should "access values with inferred types" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    import PersonSampleData.Schema

    val db = conn.database()

    val eid = Datomic.q(PersonSampleData.queryPersonIdByName, db, "tata").head.asInstanceOf[Long]

    val entity = db.entity(eid)

    entity.apply(Schema.nameAttr) should equal (PersonSampleData.tata.name)

    entity.get(Schema.idAttr) should be (None)

    entity.get(Schema.ageAttr).value should be (PersonSampleData.tata.age)
  }


  it should "access values with a type safe cast" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    import PersonSampleData.Schema

    val db = conn.database()

    val eid = Datomic.q(PersonSampleData.queryPersonIdByName, db, "tata").head.asInstanceOf[Long]

    val entity = db.entity(eid)

    entity.read[String](Schema.nameAttr) should equal (PersonSampleData.tata.name)

    entity.read[Set[Keyword]](Schema.moodAttr) should equal (PersonSampleData.tata.moods.map(_.ident))

    entity.readOpt[Long](Schema.idAttr) should be (None)

    entity.readOpt[Long](Schema.ageAttr).value should be (PersonSampleData.tata.age)

    entity.readWithDefault(Schema.nameAttr, "foo") should equal (PersonSampleData.tata.name)

    entity.readWithDefault(Schema.idAttr, 0) should equal (0)

    entity.read[Int](Schema.ageAttr) should be (PersonSampleData.tata.age.toInt)

  }

}
