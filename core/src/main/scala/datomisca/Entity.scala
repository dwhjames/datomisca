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


object Entity extends DatomicTypeWrapper with EntityOpsMacros {
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
  def retract[T](id: T)(implicit ev: AsPermanentEntityId[T]) =
    RetractEntity(ev.conv(id))

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
  def add[T](id: T)(props: (Keyword, DWrapper)*)(implicit ev: AsEntityId[T]) = {
    val builder = Map.newBuilder[Keyword, DatomicData]
    for (p <- props) builder += (p._1 -> p._2.asInstanceOf[DWrapperImpl].underlying)
    new AddEntity(ev.conv(id), builder.result)
  }

  /** Creates a Multiple-"Add" targeting a single [[DId]] from a simple Map[ [[Keyword]], [[DatomicData]] ]
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
  def add[T](id: T, props: Map[Keyword, DatomicData])(implicit ev: AsEntityId[T]) =
    new AddEntity(ev.conv(id), props)


  /** Creates a Multiple-"Add" targeting a single [[DId]] and using a [[PartialAddEntity]]
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
    * @param props a [[PartialAddEntity]] containing tuples (keyword, value)
    */
  def add[T](id: T, partial: PartialAddEntity)(implicit ev: AsEntityId[T]) =
    new AddEntity(ev.conv(id), partial.props)

  /** Creates a [[PartialAddEntity]] which is basically a AddToEntity without the DId part (''technical API'').
    *
    * @param props A sequence of tuple (keyword, value)
    *              where value can be a simple Scala type which can be converted into a DatomicData
    */
  def partialAdd(props: (Keyword, DWrapper)*) =
    PartialAddEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ).toMap)

}
