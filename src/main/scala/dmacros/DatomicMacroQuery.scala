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

package dmacros

import scala.reflect.macros.Context
import scala.reflect.internal.util.{Position, OffsetPosition}

object DatomicQueryMacro extends DatomicInception {

  def pureQueryImpl(c: Context)(q: c.Expr[String]) : c.Expr[PureQuery] = {
      import c.universe._

      val inc = inception(c)

      q.tree match {
        case Literal(Constant(s: String)) =>
          DatomicParser.parseQuerySafe(s) match {
            case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
              val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset =
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)
            case Right(q) => c.Expr[PureQuery]( inc.incept(q) )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
      }

  }

  def autoTypedQueryImpl(c: Context)(q: c.Expr[String]) : c.Expr[Any] = {

    import c.universe._

    val inc = inception(c)

    def pkgDatomic(tpe: String) = Select(Ident(newTermName("datomisca")), tpe)
    def pkgDatomicType(tpe: String) = Select(Ident(newTermName("datomisca")), newTypeName(tpe))

    q.tree match {
      case Literal(Constant(s: String)) =>
        DatomicParser.parseQuerySafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]
            val offsetPos = new OffsetPosition(
              treePos.source,
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)

          case Right(query) =>
            val insz = query.in.map( _.inputs.size ).getOrElse(0)
            val outsz = query.find.outputs.size

            /*println( showRaw(
              reify(
                datomisca.TypedQuery[datomisca.Args0, datomisca.Args2](query)
              )
            ) )*/

            val tree = c.Expr[Any](
              Apply(
                TypeApply(
                  Select(
                    pkgDatomic("TypedQueryAuto"+insz),
                    newTermName("apply")
                  ),
                  List.fill(insz)(pkgDatomicType("DatomicData")) :+
                  (outsz match {
                    case 0 => Ident(newTypeName("Unit"))
                    case 1 => Ident(newTypeName("DatomicData"))
                    case n => AppliedTypeTree(
                                Ident(newTypeName("Tuple" + n )),
                                List.fill(outsz)(pkgDatomicType("DatomicData"))
                              )
                  })
                ),
                List(inc.incept(query))
              )
            )

            //println( "Tree:"+showRaw(tree) )

            tree
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }

  }

  def KWImpl(c: Context)(q: c.Expr[String]) : c.Expr[Keyword] = {
    import c.universe._

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

  def rulesImpl(c: Context)(q: c.Expr[String]) : c.Expr[DRuleAliases] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) =>
        DatomicParser.parseDRuleAliasesSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source,
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(kw) => c.Expr[DRuleAliases]( inc.incept(kw) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }

  }

}
