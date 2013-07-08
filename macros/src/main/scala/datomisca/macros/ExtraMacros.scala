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
import scala.reflect.internal.util.{Position, OffsetPosition}


private[datomisca] trait ExtraMacros {
  /** Macro-based helper to create Datomic keyword using Clojure-style
    * {{{val kw = KW(":person/name")}}}
    *
    * @param q the Clojure string
    * @return parsed [[Keyword]]
    */
  def KW(q: String): Keyword = macro ExtraMacros.KWImpl
}

private[datomisca] object ExtraMacros {

  def KWImpl(c: Context)(q: c.Expr[String]) : c.Expr[Keyword] = {
    import c.universe._
    import DatomicInception._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) =>
        DatomicParser.parseKeywordSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source,
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(kw) => c.Expr[Keyword]( inc.incept(kw) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }

  }

}
