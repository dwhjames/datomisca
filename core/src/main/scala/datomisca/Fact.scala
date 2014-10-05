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


object Fact {
  /** Creates a single assertion about the given entity id `id`.
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Fact.add(DId(Partition.USER))( person / "name" -> "toto")
    * }}}
    *
    * @param id a value that can be used as an entity id ([[AsEntityId]])
    * @param prop a tuple ([[Keyword]], `U`)<br/>
    *             where value can be of any type that can be cast to a Datomic value type ([[ToDatomicCast]])
    */
  def add[T, U](id: T)(prop: (Keyword, U))(implicit ev0: AsEntityId[T], ev1: ToDatomicCast[U]) =
    new AddFact(ev0.conv(id), prop._1, ev1.to(prop._2))

  /** Creates a single retraction about the given entity id `id`.
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Fact.retract(eid)( person / "name" -> "toto")
    * }}}
    *
    * @param id a value that can be used as a permament entity id ([[AsPermanentEntityId]])
    * @param prop a tuple ([[Keyword]], `U`)<br/>
    *             where value can be of any type that can be cast to a Datomic value type ([[ToDatomicCast]])
    */
  def retract[T, U](id: T)(prop: (Keyword, U))(implicit ev0: AsPermanentEntityId[T], ev1: ToDatomicCast[U]) =
    new RetractFact(ev0.conv(id), prop._1, ev1.to(prop._2))

  /** Creates a special [[AddEntity]] for creating a new [[Partition]]
    *
    * {{{
    * val addPartOp = Fact.partition(Partition(my_ns / "mypart"))
    * }}}
    *
    * @param partition the partition to create
    */
  def partition(partition: Partition) =
    new AddEntity(DId(Partition.DB), Map(
      Namespace.DB / "ident"              -> partition.keyword,
      Namespace.DB.INSTALL / "_partition" -> Partition.DB.keyword
    ))

}
