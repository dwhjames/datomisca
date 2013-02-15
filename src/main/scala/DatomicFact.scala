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
import language.experimental.macros

import dmacros._

case class Fact(id: DId, attr: Keyword, value: DatomicData)

object Fact extends FactOps

object Entity extends EntityOps 

trait FactOps extends DatomicTypeWrapper {
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
    * val totoName = Datomic.Fact.retract(DId(Partition.USER))( person / "name" -> "toto")
    * }}}
    * 
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: DId)(prop: (Keyword, DWrapper)) = RetractFact(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

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
  def retract(id: DLong)(prop: (Keyword, DWrapper)) = RetractFact(DId(id), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

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

  /** Creates a single Retract operation targeting a given [[datomisca.DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.Fact.retract(3L)( person / "name" -> "toto")
    * }}}
    * 
    * @param id the [[Long]] of the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: Long)(prop: (Keyword, DWrapper)) = RetractFact(DId(DLong(id)), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

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

trait EntityOps extends DatomicTypeWrapper {
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
  def retract(id: DLong) = RetractEntity(id)

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
  def retract(id: Long) = RetractEntity(DLong(id))

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
  def retract(id: FinalId) = RetractEntity(DLong(id.underlying))

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

  /** Macro-based helper to create Datomic AddToEntity compiled from a Clojure String extended with Scala variables.
    *
    * You can then directly copy some Clojure code in a String and get it compiled.
    * You can also use variables in this String in String interpolation style.
    *
    * {{{
    * val name = "toto"
    * val weak = AddIdent(Keyword(person.character, "weak"))
    * val dumb = AddIdent(Keyword(person.character, "dumb"))
    *
    * Datomic.Entity.add("""{
    *   :db/id \${DId(Partition.USER)}
    *   :person/name \$name
    *   :person/age 30
    *   :person/character [ \$weak \$dumb ]
    * }""")
    * }}}
    *
    * @param q the Clojure string
    * @return the operation
    */
  def add(q: String): AddEntity = macro DatomicMacroOps.addEntityImpl

  
}
