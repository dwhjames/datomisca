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

package reactivedatomic

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

  def typedQueryImpl[A <: Args : c.WeakTypeTag, B <: Args : c.WeakTypeTag](c: Context)(q: c.Expr[String]) : c.Expr[TypedQuery[A, B]] = {
    def verifyInputs(query: Query): Option[PositionFailure] = {
      val tpe = implicitly[c.WeakTypeTag[A]].tpe
      val sz = query.in.map( _.inputs.size ).getOrElse(0)
      lazy val argPos = c.macroApplication.children(0).children(1).pos
      query.in.flatMap{ in => 
        if(
          (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2) 
          || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
        ) {
          Some(PositionFailure("Query Error in \":in\" : Expected %d INPUT variables".format(sz), 1, argPos.column))
        }
        else None
      }
      
    }

    def verifyOutputs(query: Query): Option[PositionFailure] = {
      val tpe = implicitly[c.WeakTypeTag[B]].tpe
      val sz = query.find.outputs.size
      val argPos = c.macroApplication.children(0).children(2).pos

      if(
        (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2)
        || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
      )
        Some(PositionFailure("Query Error in \":find\" : Expected %d OUTPUT variables".format(sz), 1, argPos.column))
      else None
    }

    def verifyTypes(query: Query): Option[PositionFailure] = {
      verifyInputs(query) match {
        case Some(p) => Some(p)
        case None => verifyOutputs(query) 
      }
    }

    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseQuerySafe(s).right.flatMap{ (t: PureQuery) => verifyTypes(t) match {
          case Some(p: PositionFailure) => Left(p)
          case None => Right(t)
        } } match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            /*val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

            val enclosingOffset = 
              enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

            val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)*/
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]
            println("treePos:"+treePos)
            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )

            println("offsetPos:"+offsetPos)
            c.abort(offsetPos.asInstanceOf[c.Position], msg)

          case Right(t) => c.Expr[TypedQuery[A, B]]( inc.incept(TypedQuery[A, B](t)) )
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