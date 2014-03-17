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


object PersonSampleData extends SampleData {

  object Schema {

    object ns {
      val person = new Namespace("person") {
        val mood = Namespace("person.mood")
      }
    }

    val idAttr = Attribute(ns.person / "id", SchemaType.long, Cardinality.one).withUnique(Unique.identity)
    val nameAttr = Attribute(ns.person / "name", SchemaType.string, Cardinality.one)
                    .withDoc("A person's name")
                    .withFullText(true)
    val ageAttr  = Attribute(ns.person / "age",  SchemaType.long, Cardinality.one)
                    .withDoc("A Person's age")
    val moodAttr = Attribute(ns.person / "mood", SchemaType.ref, Cardinality.many)
                    .withDoc("A person's mood")

    val happyMood    = AddIdent(ns.person.mood / "happy")
    val sadMood      = AddIdent(ns.person.mood / "sad")
    val excitedMood  = AddIdent(ns.person.mood / "excited")
    val stressedMood = AddIdent(ns.person.mood / "stressed")
    val angryMood    = AddIdent(ns.person.mood / "angry")

  }
  import Schema._

  override val schema = Seq(
    idAttr, nameAttr, ageAttr, moodAttr,
    happyMood, sadMood, excitedMood,
    stressedMood, angryMood
  )


  val toto = new {
    val id  = 123
    val name  = "toto"
    val age   = 30L
    val moods = Set(happyMood, excitedMood)
  }

  val totoTxData = (
    SchemaEntity.newBuilder
      += (idAttr -> toto.id)
      += (nameAttr -> toto.name)
      += (ageAttr  -> toto.age)
      ++= (moodAttr -> toto.moods)
  ) withId DId(Partition.USER)

  val tutu = new {
    val name  = "tutu"
    val age   = 54L
    val moods = Set(sadMood, stressedMood)
  }

  val tutuTxData = (
    SchemaEntity.newBuilder
      += (nameAttr -> tutu.name)
      += (ageAttr  -> tutu.age)
      ++= (moodAttr -> tutu.moods)
  ) withId DId(Partition.USER)

  val tata = new {
    val name  = "tata"
    val age   = 23L
    val moods = Set(excitedMood, angryMood)
  }

  val tataTxData = (
    SchemaEntity.newBuilder
      += (nameAttr -> tata.name)
      += (ageAttr  -> tata.age)
      ++= (moodAttr -> tata.moods)
  ) withId DId(Partition.USER)

  override val txData = Seq(
    totoTxData, tutuTxData, tataTxData
  )

  val queryPersonIdByName = Query(s"""
    [:find ?e
     :in $$ ?name
     :where [?e ${nameAttr} ?name]]
  """)
}
