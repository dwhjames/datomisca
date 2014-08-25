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
import scala.reflect.macros.whitebox.Context

import scala.collection.JavaConverters._
import scala.collection.mutable

import clojure.lang.Keyword
import clojure.{lang => clj}


private[datomisca] object MacroImpl {

  // class loader hack to get Clojure to initialize
  private def withClojure[T](block: => T): T = {
    val t = Thread.currentThread()
    val cl = t.getContextClassLoader
    t.setContextClassLoader(this.getClass.getClassLoader)
    try block finally t.setContextClassLoader(cl)
  }


  private def abortWithMessage(c: Context, message: String) =
    c.abort(c.enclosingPosition, message)


  private def abortWithThrowable(c: Context, throwable: Throwable) =
    c.abort(c.enclosingPosition, throwable.getMessage)


  private def readEDN(c: Context, edn: String): AnyRef =
    try {
      withClojure { datomic.Util.read(edn) }
    } catch {
      case ex: RuntimeException =>
        abortWithThrowable(c, ex)
    }


  def KWImpl(c: Context)(str: c.Expr[String]): c.Expr[Keyword] = {
    import c.universe._

    str.tree match {
      case Literal(Constant(s: String)) =>
        readEDN(c, s) match {
          case kw: Keyword =>
            val helper = new Helper[c.type](c)
            c.Expr[Keyword](helper.literalCljKeyword(kw))
          case _ =>
            abortWithMessage(c, "Not a valid Clojure keyword")
        }
      case _ =>
        abortWithMessage(c, "Expected a string literal")
    }
  }


  def cljRulesImpl(c: Context)(edn: c.Expr[String]): c.Expr[QueryRules] = {
    import c.universe._

    edn.tree match {
      case Literal(Constant(s: String)) =>
        val edn = readEDN(c, s)
        validateCljRules(c, edn)
        val helper = new Helper[c.type](c)
        helper.literalQueryRules(helper.literalEDN(edn))

      case q"scala.StringContext.apply(..$parts).s(..$args)" =>
        val partsWithPlaceholders = q"""Seq(..$parts).mkString(" ! ")"""
        val strWithPlaceHolders = c.eval(c.Expr[String](c.untypecheck(partsWithPlaceholders.duplicate)))
        val edn = readEDN(c, strWithPlaceHolders)
        validateCljRules(c, edn)
        val argsStack = mutable.Stack.concat(args)
        val helper = new Helper[c.type](c)
        helper.literalQueryRules(helper.literalEDN(edn, argsStack))

      case _ =>
        abortWithMessage(c, "Expected a string literal")
    }
  }


  private def validateCljRules(c: Context, edn: AnyRef): Unit =
    edn match {
      case vector: clj.PersistentVector =>
        vector.iterator.asScala foreach {
          case vector: clj.PersistentVector =>
            if (vector.count == 0) abortWithMessage(c, "Expected a rule as a non-empty vector of clauses, found an empty rule")
            vector.iterator.asScala foreach { x =>
              if (x.isInstanceOf[clj.IPersistentVector] || x.isInstanceOf[clj.IPersistentList])
                if (x.asInstanceOf[clj.IPersistentCollection].count > 0)
                  ()
                else
                  abortWithMessage(c, s"Expected a clause as a non-empty vector or list, found an empty clause")
              else
                abortWithMessage(c, s"Expected a clause as a vector or list, found value $x with ${x.getClass}")
            }
          case x =>
            abortWithMessage(c, s"Expected a rule as a vector, found value $x with ${x.getClass}")
        }
      case x =>
        abortWithMessage(c, s"Expected a vector of rules, found value $x with ${x.getClass}")
    }


  def cljQueryImpl(c: Context)(edn: c.Expr[String]): c.Expr[AbstractQuery] = {
    import c.universe._

    edn.tree match {
      case Literal(Constant(s: String)) =>
        val edn = readEDN(c, s)

        val (query, inputSize, outputSize) = validateDatalog(c, edn)
        val helper = new Helper[c.type](c)
        helper.literalQuery(helper.literalEDN(query), inputSize, outputSize)

      case q"scala.StringContext.apply(..$parts).s(..$args)" =>
        val partsWithPlaceholders = q"""Seq(..$parts).mkString(" ! ")"""
        val strWithPlaceHolders = c.eval(c.Expr[String](c.untypecheck(partsWithPlaceholders.duplicate)))
        val edn = readEDN(c, strWithPlaceHolders)
        val argsStack = mutable.Stack.concat(args)
        val (query, inputSize, outputSize) = validateDatalog(c, edn)
        val helper = new Helper[c.type](c)
        val t = helper.literalEDN(query, argsStack)
        helper.literalQuery(t, inputSize, outputSize)

      case t =>
        abortWithMessage(c, "Expected a string literal")
    }
  }


  private def validateDatalog(c: Context, edn: AnyRef): (AnyRef, Int, Int) = {
    val query = edn match {
      case coll: clj.IPersistentMap =>
        coll
      case coll: clj.PersistentVector =>
        val iter = coll.iterator.asScala.asInstanceOf[Iterator[AnyRef]]
        transformQuery(c, iter)
      case _ =>
        abortWithMessage(c, "Expected a datalog query represented as either a map or a vector")
    }

    val outputSize = Option {
        query.valAt(clj.Keyword.intern(null, "find"))
      } map { findClause =>
        findClause.asInstanceOf[clj.IPersistentVector].length
      } getOrElse { abortWithMessage(c, "The :find clause is empty") }
    val inputSize = Option {
        query.valAt(clj.Keyword.intern(null, "in"))
      } map { inClause =>
        inClause.asInstanceOf[clj.IPersistentVector].length
      } getOrElse 0

    (query, inputSize, outputSize)
  }


  private def transformQuery(c: Context, iter: Iterator[AnyRef]): clj.IPersistentMap = {
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
            abortWithMessage(c, s"Expected a query clause, found value $x with ${x.getClass}")
        }
      else
        abortWithMessage(c, "Expected a non-empty vector")

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
                abortWithMessage(c, s"Unexpected keyword $kw in datalog query")

          case o =>
            buf += o
        }
      }

      if (buf.isEmpty)
        abortWithMessage(c, s"The $clauseKW clause is empty")

      map.assoc(clauseKW, clj.PersistentVector.create(buf.asJava))
    }

    map.persistent()
  }

}
