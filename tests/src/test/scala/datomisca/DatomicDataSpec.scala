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


@RunWith(classOf[JUnitRunner])
class DatomicDataSpec extends Specification {
  "DatomicData" should {
    "extract KW from DRef" in {
      val ns = Namespace("foo")
      DRef(ns / "bar") match {
        case DRef.IsKeyword(kw) if kw == Datomic.KW(":foo/bar") => success
        case _ => failure
      }
    }
    "extract ID from DRef" in {
      DRef(DId(Partition.USER)) match {
        case DRef.IsId(id) => success
        case _ => failure
      }
    }
  }
}
