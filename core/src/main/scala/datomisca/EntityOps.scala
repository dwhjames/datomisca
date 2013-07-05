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

import macros.EntityOpsMacros


object Entity extends EntityOps

trait EntityOps extends DatomicTypeWrapper with EntityOpsMacros {
  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    *
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    *
    * {{{
    * val retractEntity = Datomic.Entity.retract(DLong(3L))
    * }}}
    *
    * @param id the DLong of a targeted real [[DId]]
    */
  def retract(id: DLong) = RetractEntity(id.underlying)

  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    *
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    *
    * {{{
    * val retractEntity = Datomic.Entity.retract(3L)
    * }}}
    *
    * @param id the long of a targeted real [[DId]]
    */
  def retract(id: Long) = RetractEntity(id)

  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    *
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    *
    * {{{
    * val retractEntity = Datomic.Entity.retract(DId(3L))
    * }}}
    *
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    */
  def retract(id: FinalId) = RetractEntity(id.underlying)

  /** Creates a Multiple-"Add" targeting a single [[DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ...
    * }
    * }}}
    *
    * {{{
    * val toto = Datomic.Entity.add(DId(Partition.USER))(
    *   person / "name" -> "toto",
    *   person / "age" -> 30L
    * )
    * }}}
    *
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    */
  def add(id: DId)(props: (Keyword, DWrapper)*) =
    AddEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ): _*)

  /** Creates a Multiple-"Add" targeting a single [[datomisca.DId]] from a simple Map[[[datomisca.Keyword]], [[datomisca.DatomicData]]]
    *
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ...
    * }
    * }}}
    *
    * {{{
    * val toto = Datomic.Entity.add(DId(Partition.USER), Map(
    *   person / "name" -> DString("toto"),
    *   person / "age" -> DLong(30L)
    * ))
    * }}}
    *
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param props the map containing all tupled (keyword, value)
    */
  def add(id: DId, props: Map[Keyword, DatomicData]) = AddEntity(id, props)


  /** Creates a Multiple-"Add" targeting a single [[DId]] and using a [[PartialAddToEntity]]
    * which is basically a AddEntity without the DId part (''technical API'').
    *
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ...
    * }
    * }}}
    *
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param props a PartialAddToEntity containing tuples (keyword, value)
    */
  def add(id: DId, props: PartialAddEntity) = AddEntity(id, props)

  /** Creates a [[PartialAddEntity]] which is basically a AddToEntity without the DId part (''technical API'').
    *
    * @param props A sequence of tuple (keyword, value)
    *              where value can be a simple Scala type which can be converted into a DatomicData
    */
  def partialAdd(props: (Keyword, DWrapper)*) =
    PartialAddEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ).toMap)

}
