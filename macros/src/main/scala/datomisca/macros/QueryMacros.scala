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
package macros

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.collection.JavaConverters._
import scala.collection.mutable

import clojure.{lang => clj}


private[datomisca] trait QueryMacros {

  /** Creates a macro-based compile-time typed query from a String:
    *    - syntax validation is performed.
    *    - determines number of input(M) parameters
    *    - determines number of output(N) parameters
    *    - creates a TypedQueryN[DatomicData, ...M-times, TupleN[DatomicData, ...N-times]]
    *
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    *
    * When an [[AbstractQuery]] is executed, it returns a `Iterable[TupleN[DatomicData, DatomicData, ...]]` where X corresponds
    * to the number of output parameters
    *
    * {{{
    * val q = Datomic.Query("""
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
  def apply(edn: String) = macro QueryMacros.cljQueryImpl


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
  def rules(edn: String) = macro QueryMacros.cljRulesImpl
}

private[datomisca] object QueryMacros {

  // class loader hack to get Clojure to initialize
  private def withClojure[T](block: => T): T = {
    val t = Thread.currentThread()
    val cl = t.getContextClassLoader
    t.setContextClassLoader(this.getClass.getClassLoader)
    try block finally t.setContextClassLoader(cl)
  }


  def cljRulesImpl(c: Context)(edn: c.Expr[String]): c.Expr[DRules] = {
    import c.universe._

    edn.tree match {
      case Literal(Constant(s: String)) =>
        val edn = withClojure { datomic.Util.read(s) }
        val helper = new Helper[c.type](c)
        helper.literalQueryRules(helper.literalEDN(edn))
      case _ =>
        c.abort(c.enclosingPosition, "Expected a string literal")
    }
  }


  def cljQueryImpl(c: Context)(edn: c.Expr[String]): c.Expr[AbstractQuery] = {
    import c.universe._

    edn.tree match {
      case Literal(Constant(s: String)) =>
        val edn = withClojure { datomic.Util.read(s) }

        try {
          val (query, inputSize, outputSize) = validateDatalog(edn)
          val helper = new Helper[c.type](c)
          helper.literalQuery(helper.literalEDN(query), inputSize, outputSize)
        } catch {
          case ex: IllegalArgumentException =>
            c.abort(c.enclosingPosition, ex.getMessage)
        }

      case _ =>
        c.abort(c.enclosingPosition, "Expected a string literal")
    }
  }


  private def validateDatalog(edn: AnyRef): (AnyRef, Int, Int) = {
    val query = edn match {
      case coll: clj.IPersistentMap =>
        coll
      case coll: clj.PersistentVector =>
        val iter = coll.iterator.asScala.asInstanceOf[Iterator[AnyRef]]
        transformQuery(iter)
      case _ =>
        throw new IllegalArgumentException("Expected a datalog query represented as either a map or a vector")
    }

    val outputSize = Option {
        query.valAt(clj.Keyword.intern(null, "find"))
      } map { findClause =>
        findClause.asInstanceOf[clj.IPersistentVector].length
      } getOrElse { throw new IllegalArgumentException("The :find clause is empty")}
    val inputSize = Option {
        query.valAt(clj.Keyword.intern(null, "in"))
      } map { inClause =>
        inClause.asInstanceOf[clj.IPersistentVector].length
      } getOrElse 0

    (query, inputSize, outputSize)
  }


  private def transformQuery(iter: Iterator[AnyRef]): clj.IPersistentMap = {
    def isQueryKeyword(kw: clj.Keyword): Boolean = {
      val name = kw.getName
      (name == "find") || (name == "with") || (name == "in") || (name == "where")
    }
    var currKW: clj.Keyword =
      if (iter.hasNext)
        iter.next() match {
          case kw: clj.Keyword if isQueryKeyword(kw) =>
            kw
          case x =>
            throw new IllegalArgumentException(s"Expected a query clause, found $x")
        }
      else
        throw new IllegalArgumentException("Expected a non-empty vector")

    val map = new clj.PersistentArrayMap(Array.empty).asTransient()
    while (iter.hasNext) {
      val clauseKW = currKW
      val buf = mutable.Buffer.empty[AnyRef]
      var shouldContinue = true

      while (shouldContinue && iter.hasNext) {
        iter.next() match {
          case kw: clj.Keyword =>
            if (isQueryKeyword(kw)) {
              currKW = kw
              shouldContinue = false
            } else
                throw new IllegalArgumentException(s"Unexpected keyword $kw in datalog query")

          case o =>
            buf += o
        }
      }

      if (buf.isEmpty)
        throw new IllegalArgumentException(s"The $clauseKW clause is empty")

      map.assoc(clauseKW, clj.PersistentVector.create(buf.asJava))
    }

    map.persistent()
  }

}
