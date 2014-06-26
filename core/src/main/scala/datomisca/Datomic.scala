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

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}

import clojure.{lang => clj}


/** Main object containing:
  *    - all Datomic basic functions (Peer, Transactor)
  *    - all Scala basic functions
  *    - all Scala high-level functions (macro, typed ops)
  *
  */
object Datomic
  extends PeerOps
     with TransactOps
     with DatomicFacilities
     with QueryExecutor
     with macros.ExtraMacros

/** Provides all Datomic Scala specific facilities
  */
private[datomisca] trait DatomicFacilities {

  /** Converts any value to a DatomicData given there is the right [[ToDatomicCast]] in the scope
    *
    * {{{
    * val s: DString = Datomic.toDatomic("toto")
    * val l: DLong = Datomic.toDatomic("5L")
    * }}}
    */
  def toDatomic[T](t: T)(implicit tdc: ToDatomicCast[T]): AnyRef = tdc.to(t)

  /** converts a DatomicData to a type given there is the right implicit in the scope
    *
    * {{{
    * val l: String = Datomic.fromDatomic(DString("toto"))
    * val s: Long = Datomic.fromDatomic(DLong(5L))
    * }}}
    */
  def fromDatomic[DD <: AnyRef, T](dd: DD)(implicit fd: FromDatomicInj[DD, T]): T = fd.from(dd)

  /** Helper: creates a [[DColl]] from simple types using DWrapper implicit conversion
    *
    * {{{
    * val addPartOp = Datomic.coll("toto", 3L, "tata")
    * }}}
    *
    * @param partition the partition to create
    */
  def list(dw: DWrapper*) = datomic.Util.list(dw.map(_.asInstanceOf[DWrapperImpl].underlying):_*).asInstanceOf[java.util.List[AnyRef]]

  /** Runtime-based helper to create multiple Datomic Operations (Add, Retract, RetractEntity, AddToEntity)
    * compiled from a Clojure String. '''This is not a Macro so no variable in string and it is evaluated
    * at runtime'''
    *
    * You can then directly copy some Clojure code in a String and get it parsed at runtime. This is why
    * it returns a `Try[Seq[TxData]]`
    * It also manages comments.
    *
    * {{{
    * val ops = Datomic.parseOps("""
    * ;; comment blabla
    *   [:db/add #db/id[:db.part/user] :db/ident :character/weak]
    *   ;; comment blabla
    *   [:db/add #db/id[:db.part/user] :db/ident :character/dumb]
    *   [:db/add #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retract #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retractEntity 1234]
    *   ;; comment blabla
    *   {
    *     :db/id #db/id[:db.part/user]
    *     :person/name "toto, tata"
    *     :person/age 30
    *     :person/character [ :character/_weak :character/dumb-toto ]
    *   }
    *   { :db/id #db/id[:db.part/user], :person/name "toto",
    *     :person/age 30, :person/character [ :character/_weak, :character/dumb-toto ]
    *   }
    * """)
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations or an error
    */
  def parseOps(ops: String): Try[Seq[TxData]] = Try {
    datomic.Util.readAll(new java.io.StringReader(ops)).asInstanceOf[java.util.List[AnyRef]].asScala map { obj =>
      new TxData {
        override val toTxData = obj
      }
    }
  }

}
