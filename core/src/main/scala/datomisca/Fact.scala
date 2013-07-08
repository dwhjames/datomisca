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


object Fact extends DatomicTypeWrapper {
  /** Creates a single Add operation targeting a given [[DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.Fact.add(DId(Partition.USER))( person / "name" -> "toto")
    * }}}
    *
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: DId)(prop: (Keyword, DWrapper)) = AddFact(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Add operation targeting a given [[DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.Fact.add(DLong(3L))( person / "name" -> "toto")
    * }}}
    *
    * @param id a DLong corresponding to a real [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: DLong)(prop: (Keyword, DWrapper)) = AddFact(DId(id), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Add operation targeting a given [[DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.Fact.add(3L)( person / "name" -> "toto")
    * }}}
    *
    * @param id a Long corresponding to a real [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: Long)(prop: (Keyword, DWrapper)) = AddFact(DId(DLong(id)), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Retract operation targeting a given [[DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.Fact.retract(DLong(3L))( person / "name" -> "toto")
    * }}}
    *
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: DLong)(prop: (Keyword, DWrapper)) = RetractFact(id.underlying, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Retract operation targeting a given [[datomisca.DId]]
    *
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.Fact.retract(3L)( person / "name" -> "toto")
    * }}}
    *
    * @param id the Long of the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: Long)(prop: (Keyword, DWrapper)) = RetractFact(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Helper: creates a special AddToEntity for creating a new Partition
    *
    * {{{
    * val addPartOp = Datomic.addPartition(Partition(Namespace.DB.PART / "mypart"))
    * }}}
    *
    * @param partition the partition to create
    */
  def partition(partition: Partition) = {
    AddEntity(DId(Partition.DB))(
      Namespace.DB / "ident" -> DString(partition.toString),
      Namespace.DB.INSTALL / "_partition" -> DString("db.part/db")
    )
  }
}
