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


class ExcisionSpec
  extends FlatSpec
     with Matchers
     with OptionValues
     with ScalaFutures
     with DatomicFixture
{

  "Datomic’s excision" can "excise specific entities" in withSampleDatomicDB(PersonSampleData) { implicit conn =>

    val dbBefore = conn.database

    val DLong(e) = Datomic.q(PersonSampleData.queryPersonIdByName, dbBefore, DString(PersonSampleData.toto.name)).head

    val excisionId = DId(Partition.USER)
    whenReady(
      Datomic.transact(Excise.entity(e, excisionId))
    ) { txReport =>
      val exId = txReport.resolve(excisionId)
      val dbAfter = txReport.dbAfter

      Datomic.q(
        Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """),
        dbAfter,
        DLong(e)).headOption.value should be (DLong(exId))
    }
  }

  it can "excise specific attributes of an entity" in withSampleDatomicDB(PersonSampleData) { implicit conn =>

    val dbBefore = conn.database

    val DLong(e) = Datomic.q(PersonSampleData.queryPersonIdByName, dbBefore, DString(PersonSampleData.toto.name)).head

    val excisionId = DId(Partition.USER)
    whenReady(
      Datomic.transact(Excise.entity(e, excisionId, PersonSampleData.Schema.ageAttr.ident))
    ) { txReport =>
      val exId = txReport.resolve(excisionId)
      val dbAfter = txReport.dbAfter

      Datomic.q(
        Query(s"""
          [:find ?e
           :in $$ ?excised
           :where
            [?e :db/excise ?excised]
            [?e :db.excise/attrs ${PersonSampleData.Schema.ageAttr}]]
        """),
        dbAfter,
        DLong(e)).headOption.value should be (DLong(exId))
    }
  }

  it can "excise values of an attribute before a date" in withSampleDatomicDB(PersonSampleData) { implicit conn =>

    val excisionId = DId(Partition.USER)
    val before = new java.util.Date
    whenReady(
      Datomic.transact(Excise.attribute(PersonSampleData.Schema.ageAttr.ident, excisionId, before))
    ) { txReport =>
      val exId = txReport.resolve(excisionId)
      val dbAfter = txReport.dbAfter

      Datomic.q(
        Query("""
          [:find ?e
           :in $ ?excised ?date
           :where
            [?e :db/excise ?excised]
            [?e :db.excise/before ?date]]
        """),
        dbAfter,
        DKeyword(PersonSampleData.Schema.ageAttr.ident),
        DInstant(before)).headOption.value should be (DLong(exId))
    }
  }

  it can "excise values of an attribute before a basis T" in withSampleDatomicDB(PersonSampleData) { implicit conn =>

    val excisionId = DId(Partition.USER)
    val beforeT = conn.database.basisT
    whenReady(
      Datomic.transact(Excise.attribute(PersonSampleData.Schema.ageAttr.ident, excisionId, beforeT))
    ) { txReport =>
      val exId = txReport.resolve(excisionId)
      val dbAfter = txReport.dbAfter

      Datomic.q(
        Query("""
          [:find ?e
           :in $ ?excised ?basisT
           :where
            [?e :db/excise ?excised]
            [?e :db.excise/beforeT ?basisT]]
        """),
        dbAfter,
        DKeyword(PersonSampleData.Schema.ageAttr.ident),
        DLong(beforeT)).headOption.value should be (DLong(exId))
    }
  }

  it can "excise all values of an attribute" in withSampleDatomicDB(PersonSampleData) { implicit conn =>

    val excisionId = DId(Partition.USER)
    whenReady(
      Datomic.transact(Excise.attribute(PersonSampleData.Schema.ageAttr.ident, excisionId))
    ) { txReport =>
      val exId = txReport.resolve(excisionId)
      val dbAfter = txReport.dbAfter

      Datomic.q(
        Query("""
          [:find ?e
           :in $ ?excised
           :where [?e :db/excise ?excised]]
        """),
        dbAfter,
        DKeyword(PersonSampleData.Schema.ageAttr.ident)).headOption.value should be (DLong(exId))
    }
  }
}
