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

import scala.concurrent.ExecutionContext.Implicits.global


class InvokeDbFnSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
     with AwaitHelper
{

  object DbFnSchema {

    val fn = new Namespace("fn")

    val myFn =
      AddDbFunction(fn / "my-fn")("h", "t")(lang = "clojure", partition = Partition.USER, imports = "", requires = "[[clojure.string :as str]]") {"""
        (str/split (str h " " t) #"\s")
      """}

    val schema = Seq(myFn)
  }

  "InvokeDbFn" should "invoke db fn in peer" in withDatomicDB { implicit conn =>
    await {
      Datomic.transact(DbFnSchema.schema)
    }

    val db = conn.database

    val res = db.invoke(DbFnSchema.myFn.ident, "The", "Quick Brown Fox")

    res.toString should equal ("""["The" "Quick" "Brown" "Fox"]""")
  }
}
