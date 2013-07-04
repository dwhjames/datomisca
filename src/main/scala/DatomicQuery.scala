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

import scala.language.higherKinds
import language.experimental.macros

import scala.util.{Try, Success, Failure}
import scala.util.parsing.input.Positional

import java.{util => ju}

import dmacros._

trait DatomicQueryExecutor extends QueryExecutorPure with QueryExecutorAuto

/* DATOMIC QUERY */
trait Query {
  def find: Find
  def wizz: Option[With] = None
  def in: Option[In] = None
  def where: Where

  override def toString = s"""[ $find ${wizz.map( _.toString + " " ).getOrElse("")}${in.map( _.toString + " " ).getOrElse("")}$where ]"""
}

object Query extends QueryMacros {
  def apply(find: Find, where: Where): PureQuery = PureQuery(find, None, None, where)
  def apply(find: Find, in: In, where: Where): PureQuery = PureQuery(find, None, Some(in), where)
  def apply(find: Find, in: Option[In], where: Where): PureQuery = PureQuery(find, None, in, where)
  def apply(find: Find, wizz: With, in: In, where: Where): PureQuery = PureQuery(find, Some(wizz), Some(in), where)
  def apply(find: Find, wizz: Option[With], in: Option[In], where: Where): PureQuery = PureQuery(find, wizz, in, where)
}


trait QueryMacros {
  /** Creates a macro-based compile-time pure query from a String (only syntax validation is performed).<br/>
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    *
    *     - A [[PureQuery]] is the low-level query AST provided in the Scala API.
    *     - [[TypedQuery]] is based on it.
    *     - When a [[PureQuery]] is executed, it returns a `List[List[DatomicData]].
    *     - All returned types are [[DatomicData]].
    *
    * {{{
    * // creates a query
    * val q = Datomic.Query.pure("""
    *  [
    *    :find ?e ?name
    *    :in $ ?char
    *    :where  [ ?e :person/name ?name ]
    *            [ ?e :person/character ?char ]
    *  ]
    * """)
    *
    * Datomic.query(q, database, person.character / "violent") map {
    *   case List(DLong(e), DString(name)) =>
    *     ...
    * }
    * }}}
    *
    * @param q the query String
    * @return a PureQuery
    */
  def pure(q: String): PureQuery = macro DatomicQueryMacro.pureQueryImpl

  /** Creates a macro-based compile-time typed query from a String:
    *    - syntax validation is performed.
    *    - determines number of input(M) parameters
    *    - determines number of output(N) parameters
    *    - creates a TypedQueryN[DatomicData, ...M-times, TupleN[DatomicData, ...N-times]]
    *
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    *
    * When a [[TypedQuery]] is executed, it returns a `List[TupleN[DatomicData, DatomicData, ...]]` where X corresponds
    * to the number of output parameters
    *
    * {{{
    * val q = Datomic.Query.auto("""
    *   [
    *    :find ?e ?name ?age
    *    :in $ [[?name ?age]]
    *    :where [?e :person/name ?name]
    *           [?e :person/age ?age]
    *   ]
    * """)
    *
    * Datomic.q(
    *   q, database,
    *   DColl(
    *     Datomic.coll("toto", 30L),
    *     Datomic.coll("tutu", 54L)
    *   )
    * ) map {
    *   case (DLong(e), DString(n), DLong(a)) =>
    *      ...
    * }
    * }}}
    */
  def auto(q: String) = macro DatomicQueryMacro.autoTypedQueryImpl
  def apply(q: String) = macro DatomicQueryMacro.autoTypedQueryImpl


  /** Macro-based helper to create Rule alias to be used in Queries.
    * {{{
    * val totoRule = Datomic.Query.rules("""
    *   [[[toto ?e]
    *     [?e :person/name "toto"]
    *   ]]
    * """)
    *
    * val q = Datomic.typedQuery[Args2, Args2]("""
    *   [
    *     :find ?e ?age
    *     :in $ %
    *     :where [?e :person/age ?age]
    *            (toto ?e)
    *   ]
    * """)
    *
    * Datomic.query(q, database, totoRule) map {
    *   case (DLong(e), DLong(age)) =>
    *     ...
    * }
    * }}}
    */
  def rules(q: String): DRuleAliases = macro DatomicQueryMacro.rulesImpl
}

case class PureQuery(override val find: Find, override val wizz: Option[With] = None, override val in: Option[In] = None, override val where: Where) extends Query

abstract class TypedQueryAuto(query: PureQuery) extends Query {
  override def find = query.find
  override def wizz = query.wizz
  override def in = query.in
  override def where = query.where
}

/* DATOMIC QUERY */
object QueryExecutor {

  private[datomisca] def directQuery(q: Query, in: Seq[AnyRef]) =
    new Iterable[IndexedSeq[DatomicData]] {
      private val jColl: ju.Collection[ju.List[AnyRef]] = datomic.Peer.q(q.toString, in: _*)
      override def iterator = new Iterator[IndexedSeq[DatomicData]] {
        private val jIter: ju.Iterator[ju.List[AnyRef]] = jColl.iterator
        override def hasNext = jIter.hasNext
        override def next() = new IndexedSeq[DatomicData] {
          private val jList: ju.List[AnyRef] = jIter.next()
          override def length = jList.size
          override def apply(idx: Int): DatomicData =
            Datomic.toDatomicData(jList.get(idx))
          override def iterator = new Iterator[DatomicData] {
            private val jIter: ju.Iterator[AnyRef] = jList.iterator
            override def hasNext = jIter.hasNext
            override def next() = Datomic.toDatomicData(jIter.next)
          }
        }
      }
    }

  private[datomisca] def directQueryOut[OutArgs](q: Query, in: Seq[AnyRef])(implicit outConv: QueryResultToTuple[OutArgs]): Iterable[OutArgs] = {
    import scala.collection.JavaConverters._
    new Iterable[OutArgs] {
      private val jColl: ju.Collection[ju.List[AnyRef]] = datomic.Peer.q(q.toString, in: _*)
      override def iterator = new Iterator[OutArgs] {
        private val jIter: ju.Iterator[ju.List[AnyRef]] = jColl.iterator
        override def hasNext = jIter.hasNext
        override def next() = outConv.toTuple(jIter.next())
      }
    }
  }
}

trait QueryExecutorPure {
  def q(query: PureQuery, in: DatomicData*): Iterable[IndexedSeq[DatomicData]] =
    QueryExecutor.directQuery(query, in.map(_.toNative))
}

trait QueryResultToTuple[T] {
  def toTuple(l: ju.List[AnyRef]): T
}

object QueryResultToTuple extends QueryResultToTupleInstances
