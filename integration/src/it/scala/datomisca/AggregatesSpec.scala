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

import org.scalatest.{FlatSpec, Matchers, OptionValues}


class AggregatesSpec
  extends FlatSpec
     with Matchers
     with OptionValues
     with DatomicFixture
{

  val countObjects = Query("""
    [:find (count ?e)
     :where [?e :object/name ?n]]
  """)

  val findLargestRadius = Query("""
    [:find (max ?radius)
     :where [_ :object/meanRadius ?radius]]
  """)

  val findSmallestRadius = Query("""
    [:find (min ?radius)
     :where [_ :object/meanRadius ?radius]]
  """)

  val findAverageRadius = Query("""
    [:find (avg ?radius)
     :with ?e
     :where [?e :object/meanRadius ?radius]]
  """)

  val findMedianRadius = Query("""
    [:find (median ?radius)
     :with ?e
     :where [?e :object/meanRadius ?radius]]
  """)

  val findStdDevOfRadius = Query("""
    [:find (stddev ?radius)
     :with ?e
     :where [?e :object/meanRadius ?radius]]
  """)

  val findRandomObject = Query("""
    [:find (rand ?name)
     :where [?e :object/name ?name]]
  """)

  val findSmallest3 = Query("""
    [:find (min 3 ?radius)
     :with ?e
     :where [?e :object/meanRadius ?radius]]
  """)

  val findLargest3 = Query("""
    [:find (max 3 ?radius)
     :with ?e
     :where [?e :object/meanRadius ?radius]]
  """)

  val findRandom5 = Query("""
    [:find (rand 5 ?name)
     :with ?e
     :where [?e :object/name ?name]]
  """)

  val choose5 = Query("""
    [:find (sample 5 ?name)
     :with ?e
     :where [?e :object/name ?name]]
  """)

  val findAvgSchemaNameLength = Query("""
    [:find (avg ?length)
     :with ?e
     :where
     [?e :db/ident ?ident]
     [(name ?ident) ?name]
     [(count ?name) ?length]]
  """)

  val countAttributesAndValueTypesInSchema = Query("""
    [:find (count ?a) (count-distinct ?vt)
     :where
     [?a :db/ident ?ident]
     [?a :db/valueType ?vt]]
  """)

  "Aggregates examples" should "run to completion" in withSampleDatomicDB(PlutoSampleData) { conn =>
    val db = conn.database

    Datomic.q(countObjects, db).headOption.value should equal (DLong(17))

    Datomic.q(findLargestRadius, db).headOption.value should equal (DDouble(696000.0))

    Datomic.q(findSmallestRadius, db).headOption.value should equal (DDouble(1163.0))

    Datomic.q(findAverageRadius, db).headOption.value should equal (DDouble(53390.17647058824))

    Datomic.q(findMedianRadius, db).headOption.value should equal (DDouble(2631.2))

    Datomic.q(findStdDevOfRadius, db).headOption.value should equal (DDouble(161902.5278094546))

    Datomic.q(findRandomObject, db) should have size (1)

    Datomic.q(findSmallest3, db).headOption.value match {
      case DColl(coll) =>
        coll should contain allOf (DDouble(1163.0), DDouble(1353.4), DDouble(1561.0))
      case _ => fail
    }

    Datomic.q(findLargest3, db).headOption.value match {
      case DColl(coll) =>
        coll should contain allOf (DDouble(696000.0), DDouble(69911.0), DDouble(58232.0))
      case _ => fail
    }

    Datomic.q(findRandom5, db) should have size (1)

    Datomic.q(choose5, db) should have size (1)

    Datomic.q(findAvgSchemaNameLength, db).headOption.value should equal (DDouble(5.846153846153846))

    Datomic.q(countAttributesAndValueTypesInSchema, db) should have size (1)
  }
}
