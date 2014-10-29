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


class RichAttributeSpec
  extends FlatSpec
     with Matchers
     with OptionValues
     with DatomicFixture
{

  "RichAttribute" should "access values with a type safe cast" in withSampleDatomicDB(PersonSampleData) { implicit conn =>
    import PersonSampleData.Schema

    val db = conn.database()

    val eid = Datomic.q(PersonSampleData.queryPersonIdByName, db, "tata").head.asInstanceOf[Long]

    val entity = db.entity(eid)

    Schema.nameAttr.read[String].read(entity) should equal (PersonSampleData.tata.name)

    Schema.moodAttr.read[Set[Keyword]].read(entity) should equal (PersonSampleData.tata.moods.map(_.ident))

    Schema.idAttr.readOpt[Long].read(entity) should be (None)

    Schema.ageAttr.readOpt[Long].read(entity).value should be (PersonSampleData.tata.age)

    Schema.nameAttr.readWithDefault("foo").read(entity) should equal (PersonSampleData.tata.name)

    Schema.idAttr.readWithDefault(0).read(entity) should equal (0)

    Schema.ageAttr.read[Int].read(entity) should be (PersonSampleData.tata.age.toInt)

  }

}
