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

import macros.{DatomicParser, PositionFailure}

import scala.util.{Try, Success, Failure}


/** Main object containing:
  *    - all Datomic basic functions (Peer, Transactor)
  *    - all Scala basic functions
  *    - all Scala high-level functions (macro, typed ops)
  *    - all implicit DDReader/DDWriter
  *
  *
  * {{{
  * import Datomic._ // brings all DDReader/DDWriter
  * }}}
  */
object Datomic
  extends PeerOps
     with TransactOps
     with DatomicFacilities
     with QueryExecutor
     with DatomicTypeWrapper
     with macros.ExtraMacros
     with macros.OpsMacros

/** Provides all Datomic Scala specific facilities
  */
trait DatomicFacilities extends DatomicTypeWrapper{

  /** Converts any value to a DatomicData given there is the right [[DDWriter]] in the scope
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val s: DString = Datomic.toDatomic("toto")
    * val l: DLong = Datomic.toDatomic("5L")
    * }}}
    */
  def toDatomic[T](t: T)(implicit tdc: ToDatomicCast[T]): DatomicData = tdc.to(t)

  /** converts a DatomicData to a type given there is the right [[DDReader]] in the scope
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val l: String = Datomic.fromDatomic(DString("toto"))
    * val s: Long = Datomic.fromDatomic(DLong(5L))
    * }}}
    */
  def fromDatomic[DD <: DatomicData, T](dd: DD)(implicit fd: FromDatomicInj[DD, T]): T = fd.from(dd)

  /** Helper: creates a [[DColl]] from simple types using DWrapper implicit conversion
    *
    * {{{
    * val addPartOp = Datomic.coll("toto", 3L, "tata")
    * }}}
    *
    * @param partition the partition to create
    */
  def coll(dw: DWrapper*) = DColl(dw.map(_.asInstanceOf[DWrapperImpl].underlying))

  /** Runtime-based helper to create multiple Datomic Operations (Add, Retract, RetractEntity, AddToEntity)
    * compiled from a Clojure String. '''This is not a Macro so no variable in string and it is evaluated
    * at runtime'''
    *
    * You can then directly copy some Clojure code in a String and get it parsed at runtime. This is why
    * it returns a `Try[Seq[Operation]]`
    * It also manages comments.
    *
    * {{{
    * val ops = Datomic.parseOps("""
    * ;; comment blabla
    * [
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
    * ]""")
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations or an error
    */
  def parseOps(q: String): Try[Seq[Operation]] = DatomicParser.parseOpSafe(q) match {
    case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
      Failure(new RuntimeException(s"Couldn't parse operations[msg:$msg, line:$offsetLine, col:$offsetCol]"))
    case Right(ops) =>
      Success(ops)
  }

}
