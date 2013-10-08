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

import ast._

import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.api
import scala.reflect.api.Liftable


private[datomisca] class Helper[C <: Context](val c: C) {
  import c.universe._

  implicit val liftBigInt: Liftable[BigInt] = new Liftable[BigInt] {
    def apply(universe: api.Universe, value: BigInt): universe.Tree =
      universe.Literal(universe.Constant(value))
  }
  implicit val liftBigDec: Liftable[BigDecimal] = new Liftable[BigDecimal] {
    def apply(universe: api.Universe, value: BigDecimal): universe.Tree =
      universe.Literal(universe.Constant(value))
  }

  def computeOffset(
    pos: scala.reflect.internal.util.Position,
    offsetLine: Int, offsetCol: Int): Int = {
    val source = pos.source
    val computedOffset = source.lineToOffset(pos.line - 1 + offsetLine - 1 )
    val isMultiLine = source.beginsWith(pos.point, "\"\"\"")

    val computedCol =
      if(offsetLine > 1 && isMultiLine) (offsetCol - 1)
      else if(isMultiLine) (offsetCol - 1 + pos.column - 1 + 3)
      else (offsetCol - 1 + pos.column - 1 + 1)

    computedOffset + computedCol
  }

  def incept[T](opt: Option[T]): c.universe.Tree = {
    opt match {
      case None => q"None"
      case Some(x) =>
        val arg = c.universe.reify(x).tree
        q"Some($arg)"
    }
  }

  def incept(d: DatomicData): c.Tree = d match {
    case DString(v)  => q"datomisca.DString($v)"
    case DLong(v)    => q"datomisca.DLong($v)"
    case DFloat(v)   => q"datomisca.DFloat($v)"
    case DDouble(v)  => q"datomisca.DDouble($v)"
    case DBoolean(v) => q"datomicsa.DBoolean($v)"
    case DRef(v) =>
      val arg = v match {
        case Left(kw) => incept(kw)
        case Right(id) => incept(id)
      }
      q"datomisca.DRef($arg)"
    case DBigInt(v) => q"datomisca.DBigInt($v)"
    case DBigDec(v) => q"datomisca.DBigDec($v)"
    case DColl(elts) =>
      val args = elts.map(incept(_)).toList
      q"datomisca.DColl(..$args)"
    case id: DId => id match {
      case FinalId(v) => q"datomisca.FinalId($v)"
      case TempId(part, id, dbId) =>
        val underlyingId = Literal(Constant(dbId))
        q"datomisca.TempId(${incept(part)}, ${incept(id)}, $underlyingId)"
    }
    /*case DInstant(v) => Apply(Ident(newTermName("DInstant")), List(Literal(Constant(v))))
    case DBytes(v) => Apply(Ident(newTermName("DBytes")), List(Literal(Constant(v))))
    case DUuid(v) => Apply(Ident(newTermName("DUuid")), List(Literal(Constant(v))))
    case DUri(v) => Apply(Ident(newTermName("DUri")), List(Literal(Constant(v))))*/
    case e => throw new RuntimeException("Unexcepted type "+e.getClass.toString+" while incepting a macro")
  }

  def incept(ds: DataSource): c.Tree = ds match {
    case ImplicitDS => q"datomisca.ImplicitDS"
    case ExternalDS(n) => q"datomisca.ExternalDS($n)"
  }

  def incept(t: Term): c.Tree = t match {
    case Var(name) => q"datomisca.Var($name)"
    case Keyword(name, None) => q"datomisca.Keyword($name)"
    case Keyword(name, Some(Namespace(ns))) =>
      q"datomisca.Keyword($name, Some(datomisca.Namespace($ns)))"
    case Empty => q"datomisca.Empty"
    case Const(d: DatomicData) =>
      q"datomisca.Const(${incept(d)})"
    case ds: DataSource => incept(ds)
  }

  def incept(part: Partition): c.Tree =
    q"datomisca.Partition(${incept(part.keyword)})"
  def incept(df: DFunction): c.Tree =
    q"datomisca.ast.DFunction(${df.name})"
  def incept(df: DPredicate): c.Tree =
    q"datomisca.ast.DPredicate(${df.name})"

  def incept(b: Binding): c.Tree = b match {
    case ScalarBinding(name) =>
      q"datomisca.ast.ScalarBinding(${incept(name)})"
    case TupleBinding(names) =>
      val args = names.map(incept(_)).toList
      q"datomisca.ast.TupleBinding(Seq(..$args))"
    case CollectionBinding(name) =>
      q"datomisca.ast.CollectionBinding(${incept(name)})"
    case RelationBinding(names) =>
      val args = names.map(incept(_)).toList
      q"datomisca.ast.RelationBinding(Seq(..$args))"
  }

  def incept(e: Expression): c.Tree = e match {
    case PredicateExpression(df, args) =>
      val fargs = args.map(incept(_)).toList
      q"datomisca.ast.PredicateExpression(${incept(df)}, Seq(..$fargs))"
    case FunctionExpression(df, args, binding) =>
      val fargs = args.map(incept(_)).toList
      q"datomisca.ast.FunctionExpression(${incept(df)}, Seq(..$fargs), ${incept(binding)})"

  }

  def incept(r: Rule): c.Tree = r match {
    case DataRule(ds, entity, attr, value, tx, added) =>
      val dsArg =
        if (ds == ImplicitDS)
          q"datomisca.ImplicitDS"
        else
          q"datomisca.ExternalDS(${ds.name})"
      q"""datomisca.ast.DataRule(
        $dsArg,
        ${incept(entity)},
        ${incept(attr)},
        ${incept(value)},
        ${incept(tx)},
        ${incept(added)}
      )"""
    case f: ExpressionRule =>
      q"datomisca.ast.ExpressionRule(${incept(f.expr)})"
    case ra: RuleAliasCall =>
      val args = ra.args.map(incept(_)).toList
      q"datomisca.ast.RuleAliasCall(${ra.name}, Seq(..$args))"
    case DataRuleParsing(ds, entity, attr, value, tx, added) =>
      val dsArg =
        if (ds == ImplicitDS)
          q"datomisca.ImplicitDS"
        else
          q"datomisca.ExternalDS(${ds.name})"
      q"""datomisca.ast.DataRule(
        $dsArg,
        ${incept(entity)},
        ${incept(attr)},
        ${incept(value)},
        ${incept(tx)},
        ${incept(added)}
      )"""
  }

  def incept(t: TermParsing): c.Tree = t.value match {
    case Left(se: ScalaExpr) => c.parse(se.expr)
    case Right(t: Term) => incept(t)
  }


  def incept(o: Output): c.Tree = o match {
    case OutVariable(v) =>
      q"datomisca.ast.OutVariable(${incept(v)})"
  }

  def incept(w: Where): c.Tree = {
    val args = w.rules.map(incept(_)).toList
    q"datomisca.ast.Where(Seq(..$args))"
  }

  def incept(i: Input): c.Tree = i match {
    case InDataSource(ds) =>
      q"datomisca.ast.InDataSource(${incept(ds)})"
    case InVariable(v) =>
      q"datomisca.ast.InVariable(${incept(v)})"
    case InRuleAlias =>
      q"datomisca.ast.InRuleAlias"
  }

  def incept(in: In): c.Tree = {
    val args = in.inputs.map(incept(_)).toList
    q"datomisca.ast.In(Seq(..$args))"
  }

  def incept(f: Find): c.Tree = {
    val args = f.outputs.map(incept(_)).toList
    q"datomisca.ast.Find(Seq(..$args))"
  }

  def incept(f: With): c.Tree = {
    val args = f.variables.map(incept(_)).toList
    q"datomisca.ast.With(Seq(..$args))"
  }

  def incept(q: PureQuery): c.universe.Tree = {
    val wizz = q.wizz match {
      case None       => q"None"
      case Some(wizz) => q"Some(${incept(wizz)})"
    }
    val in = q.in match {
      case None     => q"None"
      case Some(in) => q"Some(${incept(in)})"
    }
    q"""datomisca.PureQuery(
      ${incept(q.find)},
      $wizz,
      $in,
      ${incept(q.where)}
    )"""
  }

  def incept(se: ScalaExpr): c.universe.Tree = {
    val compiled = c.parse(se.expr)
    q"datomisca.Datomic.toDWrapper($compiled)"
  }

  def incept(seq: DCollParsing): c.universe.Tree = {
    val args = seq.elts.map {
      case Left(se: ScalaExpr)    => incept(se)
      case Right(dd: DatomicData) => incept(dd)
      case _ => throw new RuntimeException("Unexpected data while incepting DCollParsing")
    } .toList
    q"datomisca.Datomic.coll(..$args)"
  }

  private def inceptId(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
    case Left(se: ScalaExpr) => c.parse(se.expr)
    case Right(did: DId)     => incept(did)
    case _ => c.abort(c.enclosingPosition, ":db/id can only be a DId")
  }

  private def localIncept(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
    case Left(se: ScalaExpr)    => incept(se)
    case Left(se: DCollParsing) => incept(se)
    case Right(dd: DatomicData) => incept(dd)
  }

  def incept(a: AddEntityParsing): c.universe.Tree = {
    if(!a.props.contains(Keyword("id", Namespace.DB)))
      c.abort(c.enclosingPosition, "addEntity requires one :db/id field")
    else {
      val id = inceptId(a.props(Keyword("id", Namespace.DB)))
      val argPairs = (a.props - Keyword("id", Namespace.DB) ).map { case (k, v) =>
          q"Tuple2(${incept(k)}, ${localIncept(v)})"
        }.toList
      q"datomisca.AddEntity($id, Map(..$argPairs))"
    }
  }

  def incept(did: DIdParsing): c.universe.Tree = {
    val arg = incept(did.partition)
    did.id match {
      case None     => q"datomisca.DId($arg)"
      case Some(id) => q"datomisca.DId($arg, $id)"
    }
  }

  def incept(fact: FactParsing): List[c.universe.Tree] = {
    List(
      fact.id match {
        case Left(se: ScalaExpr)   => inceptId(Left(se))
        case Right(id: DIdParsing) => incept(id)
        case _ => c.abort(c.enclosingPosition, "A Fact only accepts a #db/id[:db.part/XXX] or a scala DId as 1st param")
      },
      incept(fact.attr),
      localIncept(fact.value)
    )
  }

  def incept(op: AddFactParsing): c.universe.Tree =
    q"datomisca.AddFact(..${incept(op.fact)})"

  def incept(op: RetractFactParsing): c.universe.Tree =
    q"datomisca.RetractFact(..${incept(op.fact)})"

  def incept(op: RetractEntityParsing): c.universe.Tree = {
    val arg = op.entid match {
      case Left(se: ScalaExpr) => incept(se)
      case Right(entid) => incept(entid)
      case _ => c.abort(c.enclosingPosition, "A Fact only accepts a DLong as 1st param")
    }
    q"datomisca.RetractEntity($arg)"
  }

  def incept(ra: DRuleAlias): c.universe.Tree = {
    val args = ra.args.map(incept(_)).toList
    val rules = ra.rules.map(incept(_)).toList
    q"datomisca.DRuleAlias(${ra.name}, Seq(..$args), Seq(..$rules))"
  }

  def incept(ras: DRuleAliases): c.universe.Tree = {
    val args = ras.aliases.map(incept(_)).toList
    q"datomisca.DRuleAliases(Seq(..$args))"
  }

  def incept(ops: Seq[OpParsing]): c.universe.Tree = {
    val args = ops.map{
      case add:    AddFactParsing       => incept(add)
      case ret:    RetractFactParsing   => incept(ret)
      case retEnt: RetractEntityParsing => incept(retEnt)
      case addEnt: AddEntityParsing     => incept(addEnt)
    }.toList
    q"Seq(..$args)"
  }

}
