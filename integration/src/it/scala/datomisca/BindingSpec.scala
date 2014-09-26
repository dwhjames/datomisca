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

import org.scalatest.{FlatSpec, Matchers}


class BindingSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
{

  "A Query" can "bind variables" in {
    val query = Query("""
      [:find ?first ?last
       :in ?first ?last]
    """)

    val res = Datomic.q(query, "John", "Doe")

    res should have size (1)
    res.head should equal (("John", "Doe"))
  }

  it can "bind tuples" in {
    val query = Query("""
      [:find ?first ?last
       :in [?first ?last]]
    """)

    val res = Datomic.q(query, Datomic.list("John", "Doe"))

    res should have size (1)
    res.head should equal (("John", "Doe"))
  }

  it can "bind a relation" in {
    val query = Query("""
      [:find ?first ?last
       :in [[?first ?last]]]
    """)

    val res =
      Datomic.q(query,
        Datomic.list(
          Datomic.list("John", "Doe"),
          Datomic.list("Jane", "Doe")
        )
      )

    res should have size (2)
    res should contain (("John", "Doe"))
    res should contain (("Jane", "Doe"))
  }

  it can "bind a database" in {
    val query = Query("""
      [:find ?first
       :where [_ :first-name ?first]]
    """)

    val res =
      Datomic.q(query,
        Datomic.list(
          Datomic.list(42, Datomic.KW(":first-name"), "John"),
          Datomic.list(42, Datomic.KW(":last-name"),  "Doe"),
          Datomic.list(43, Datomic.KW(":first-name"), "Jane"),
          Datomic.list(43, Datomic.KW(":last-name"),  "Doe")
        )
      )

    res should have size (2)
    res should contain ("John")
    res should contain ("Jane")
  }

  it can "bind a collection of datoms" in withDatomicDB { implicit conn =>
    val query = Query("""
        [:find ?e
         :in $ ?attrId
         :where [?e ?attrId]]
      """)

    val ds = conn.database().datoms(Database.AEVT, Attribute.doc)

    val res = Datomic.q(query, ds, conn.database().entid(Attribute.doc))

    res should contain (0)
  }

}
