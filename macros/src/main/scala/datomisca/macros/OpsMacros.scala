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


private[datomisca] trait EntityOpsMacros {
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
    * Datomic.Entity.addRaw("""{
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
  def addRaw(q: String): AddEntity = macro OpsMacros.addEntityImpl
}

trait OpsMacros {
  /** Macro-based helper to create multiple Datomic Operations (Add, Retract, RetractEntity, AddToEntity)
    * compiled from a Clojure String extended with Scala variables.
    *
    * You can then directly copy some Clojure code in a String and get it compiled.
    * You can also use variables in this String in String interpolation style.
    *
    * {{{
    * val id = DId(Partition.USER)
    *
    * val weak = AddIdent(Keyword(Namespace("person.character"), "weak"))
    * val dumb = AddIdent(Keyword(Namespace("person.character"), "dumb"))
    *
    * val id = DId(Partition.USER)
    * val ops = Datomic.ops("""[
    *   [:db/add #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/add \${DId(Partition.USER)} :db/ident :region/n]
    *   [:db/retract #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retractEntity 1234]
    *   {
    *     :db/id \${id}
    *     :person/name "toto"
    *     :person/age 30
    *     :person/character [ \$weak \$dumb ]
    *   }
    * ]""")
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations
    */
  def ops(q: String): Seq[Operation] = macro OpsMacros.opsImpl
}

private[datomisca] object OpsMacros {
  import DatomicInception._

  def addEntityImpl(c: Context)(q: c.Expr[String]): c.Expr[AddEntity] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseAddEntityParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ae) => 
            c.Expr[AddEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }

  def opsImpl(c: Context)(q: c.Expr[String]): c.Expr[Seq[Operation]] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseOpParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ops) => 
            c.Expr[Seq[Operation]]( inc.incept(ops) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }


  /*def transact(ops: String): Future[TxResult] = macro transactImpl

  def transactImpl(c: Context)(ops: c.Expr[String]): c.Expr[Future[TxResult]] = {
    import c.universe._

    val inc = inception(c)

    ops.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseAddToEntityParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = ops.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ae) => 
            c.Expr[Future[TxResult]](Apply(
              Select(Ident(newTermName("connection")), "transact"),
              List(
                inc.incept(ae)
              )
            ))
            //c.Expr[AddToEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }*/
}
