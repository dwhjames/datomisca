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

import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}

trait DatomicInception {
  def computeOffset(
    pos: scala.reflect.internal.util.Position, 
    offsetLine: Int, offsetCol: Int): Int = {
    val source = pos.source
    val computedOffset = source.lineToOffset(pos.line - 1 + offsetLine - 1 )
    val isMultiLine = source.beginsWith(pos.offset.get, "\"\"\"")

    val computedCol = 
      if(offsetLine > 1 && isMultiLine) (offsetCol - 1) 
      else if(isMultiLine) (offsetCol - 1 + pos.column - 1 + 3)
      else (offsetCol - 1 + pos.column - 1 + 1)

    computedOffset + computedCol
  }

  def inception(c: Context) = {
    import c.universe._

    new {

      def incept[T](opt: Option[T]): c.universe.Tree = {
        opt match {
          case None => Ident(newTermName("None"))
          case Some(x) => Apply(Ident(newTermName("Some")), List(c.universe.reify(x).tree))
        }
      }

      def incept(d: DatomicData): c.Tree = d match {
        case DString(v) => Apply(Ident(newTermName("DString")), List(Literal(Constant(v))))
        case DLong(v) => Apply(Ident(newTermName("DLong")), List(Literal(Constant(v))))
        case DFloat(v) => Apply(Ident(newTermName("DFloat")), List(Literal(Constant(v))))
        case DDouble(v) => Apply(Ident(newTermName("DDouble")), List(Literal(Constant(v))))
        case DBoolean(v) => Apply(Ident(newTermName("DBoolean")), List(Literal(Constant(v))))
        case DRef(v) => Apply(Ident(newTermName("DRef")), List(v match {
          case Left(kw) => incept(kw)
          case Right(id) => incept(id)
        }))
        case DBigInt(v) => Apply(Ident(newTermName("DBigInt")), List(Literal(Constant(v))))
        case DBigDec(v) => Apply(Ident(newTermName("DBigDec")), List(Literal(Constant(v))))
        //case DBigDec(v) => v.toString
        //case DInstant(v) => v.toString
        //case DUuid(v) => v.toString
        //case DUri(v) => v.toString
        
      }

      def incept(ds: DataSource): c.Tree = ds match {
        case ImplicitDS => Ident(newTermName("ImplicitDS"))
        case ExternalDS(n) => Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(n))) )         
      }

      def incept(t: Term): c.Tree = t match {
        case Var(name) => Apply( Ident(newTermName("Var")), List(Literal(Constant(name))) )
        case Keyword(name, None) => Apply( 
          Ident(newTermName("Keyword")), 
          List(
            Literal(Constant(name))
          )
        )
        case Keyword(name, Some(Namespace(ns))) => Apply( 
          Ident(newTermName("Keyword")), 
          List(
            Literal(Constant(name)), 
            Apply(Ident(newTermName("Some")), List(
              Apply(Ident(newTermName("Namespace")), List(Literal(Constant(ns)))) 
            ))
          )
        )
        case Empty => Ident(newTermName("Empty"))
        case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(incept(d)) )
        case ds: DataSource => incept(ds)
      }

      def incept(part: Partition): c.Tree = Apply( Ident(newTermName("Partition")), List(incept(part.keyword)) )
      def incept(df: DFunction): c.Tree = Apply( Ident(newTermName("DFunction")), List(Literal(Constant(df.name))) )
      def incept(df: DPredicate): c.Tree = Apply( Ident(newTermName("DPredicate")), List(Literal(Constant(df.name))) )

      def incept(b: Binding): c.Tree = b match {
        case ScalarBinding(name) => Apply( Ident(newTermName("ScalarBinding")), List(incept(name)) )
        case TupleBinding(names) => 
          Apply( 
            Ident(newTermName("TupleBinding")), 
            List(
              Apply(
                Ident(newTermName("Seq")),
                names.map(incept(_)).toList
              )
            )
          )
        case CollectionBinding(name) => Apply( Ident(newTermName("CollectionBinding")), List(incept(name)) )
        case RelationBinding(names) => 
          Apply( 
            Ident(newTermName("RelationBinding")), 
            List(
              Apply(
                Ident(newTermName("Seq")),
                names.map(incept(_)).toList
              )
            )
          )
      }

      def incept(e: Expression): c.Tree = e match {
        case PredicateExpression(df, args) => 
          Apply( 
            Ident(newTermName("PredicateExpression")), 
            List(
              incept(df),
              Apply(Ident(newTermName("Seq")), args.map(incept(_)).toList)
            )
          )
        case FunctionExpression(df, args, binding) =>
          Apply( 
            Ident(newTermName("FunctionExpression")), 
            List(
              incept(df),
              Apply(Ident(newTermName("Seq")), args.map(incept(_)).toList),
              incept(binding)
            )
          )
      }

      def incept(r: Rule): c.Tree = r match {
        case DataRule(ds, entity, attr, value, tx, added) =>
          Apply( Ident(newTermName("DataRule")), 
            List(
              (if(ds == ImplicitDS) Ident(newTermName("ImplicitDS")) else Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(ds.name)))) ), 
              incept(entity), 
              incept(attr), 
              incept(value), 
              incept(tx), 
              incept(added)
            ) 
          )
        case f: ExpressionRule => 
          Apply( 
            Ident(newTermName("ExpressionRule")), 
            List(incept(f.expr))
          )
        
        case ra: RuleAliasCall =>
          Apply(
            Ident(newTermName("RuleAliasCall")), 
            List(
              Literal(Constant(ra.name)),
              Apply(Ident(newTermName("Seq")), ra.args.map(incept(_)).toList )
            )
          )
      }

      def incept(o: Output): c.Tree = o match {
        case OutVariable(v) => Apply(Ident(newTermName("OutVariable")), List(incept(v)))
      }

      def incept(w: Where): c.Tree = 
        Apply( Ident(newTermName("Where")), List( Apply(Ident(newTermName("Seq")), w.rules.map(incept(_)).toList )) )


      def incept(i: Input): c.Tree = i match {
        case InDataSource(ds) => Apply(Ident(newTermName("InDataSource")), List(incept(ds)))
        case InVariable(v) => Apply(Ident(newTermName("InVariable")), List(incept(v)))
        case InRuleAlias => Ident(newTermName("InRuleAlias"))
      }

      def incept(in: In): c.Tree = 
        Apply( Ident(newTermName("In")), List( Apply(Ident(newTermName("Seq")), in.inputs.map(incept(_)).toList )) )

      
      def incept(f: Find): c.Tree = 
        Apply( Ident(newTermName("Find")), List( Apply(Ident(newTermName("Seq")), f.outputs.map(incept(_)).toList )) )  

      def incept(f: With): c.Tree = 
        Apply( Ident(newTermName("With")), List( Apply(Ident(newTermName("Seq")), f.variables.map(incept(_)).toList )) )  

      def incept(q: PureQuery): c.universe.Tree = {
        Apply(
          Ident(newTermName("PureQuery")), 
          List(incept(q.find)) ++ 
          q.wizz.map{ wizz => List(Apply(Ident(newTermName("Some")), List(incept(wizz)))) }.getOrElse(List(Ident(newTermName("None")))) ++ 
          q.in.map{ in => List(Apply(Ident(newTermName("Some")), List(incept(in)))) }.getOrElse(List(Ident(newTermName("None")))) ++ 
          List(incept(q.where))
        )
      }

      def incept[A <: Args, B <: Args](q: TypedQuery[A, B]): c.universe.Tree = {
        Apply(
          Ident(newTermName("TypedQuery")), 
          List(
            incept(q.query)
          )
        )
      }

      def incept(se: ScalaExpr): c.universe.Tree = {
        val compiled = c.parse(se.expr)
        Apply(Select(Ident(newTermName("Datomic")), "toDWrapper"), List(compiled))
      }

      def incept(seq: DSetParsing): c.universe.Tree = {
        Apply(
          Select(Ident(newTermName("Datomic")), "dset"),
          seq.elts.map{ 
            case Left(se: ScalaExpr) => incept(se)
            case Right(dd: DatomicData) => incept(dd)
          }.toList
        )
      }

      private def inceptId(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
        case Left(se: ScalaExpr) => c.parse(se.expr)
        case Right(did: DId) => incept(did)
        case _ => c.abort(c.enclosingPosition, ":db/id can only be a DId")
      }

      private def localIncept(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
        case Left(se: ScalaExpr) => incept(se)
        case Left(se: DSetParsing) => incept(se)
        case Right(dd: DatomicData) => incept(dd)
      }

      def incept(a: AddToEntityParsing): c.universe.Tree = {
        if(!a.props.contains(Keyword("id", Namespace.DB)))
          c.abort(c.enclosingPosition, "An AddToEntity requires one :db/id field")
        else {
          val tree = Apply(
            Apply(
              Select(Ident(newTermName("Datomic")), "addToEntity"),
              List(inceptId(a.props(Keyword("id", Namespace.DB))))
            ),
            ( a.props - Keyword("id", Namespace.DB) ).map{ case (k, v) => 
              Apply(Ident(newTermName("Tuple2")), List( incept(k), localIncept(v) ))
            }.toList
          )
          tree
        }
      }

      def incept(did: DIdParsing): c.universe.Tree = { 
        Apply(
          Ident(newTermName("DId")),
          List(
            incept(did.partition)
          ) ++ (did.id match {
            case None => List()
            case Some(id) => List(Literal(Constant(id)))
          })
        )
      }

      def incept(fact: FactParsing): c.universe.Tree = { 
        Apply(
          Ident(newTermName("Fact")),
          List(
            fact.id match {
              case Left(se: ScalaExpr) => inceptId(Left(se))
              case Right(id: DIdParsing) => incept(id)
              case _ => c.abort(c.enclosingPosition, "A Fact only accepts a #db/id[:db.part/XXX] or a scala DId as 1st param")
            },
            incept(fact.attr),
            localIncept(fact.value)
          )
        )
      }

      def incept(op: AddParsing): c.universe.Tree = {
        Apply(
          Ident(newTermName("Add")),
          List(incept(op.fact))  
        )
      }

      def incept(op: RetractParsing): c.universe.Tree = {
        Apply(
          Ident(newTermName("Retract")),
          List(incept(op.fact))  
        )
      }

      def incept(op: RetractEntityParsing): c.universe.Tree = {
        Apply(
          Ident(newTermName("RetractEntity")),
          List(
            op.entid match {
              case Left(se: ScalaExpr) => incept(se)
              case Right(entid) => incept(entid)
              case _ => c.abort(c.enclosingPosition, "A Fact only accepts a DLong as 1st param")
            }
          )  
        )
      }

      def incept(ra: DRuleAlias): c.universe.Tree = {
        Apply(
          Ident(newTermName("DRuleAlias")),
          List(
            Literal(Constant(ra.name)),
            Apply(Ident(newTermName("Seq")), ra.args.map(incept(_)).toList ),
            Apply(Ident(newTermName("Seq")), ra.rules.map(incept(_)).toList )
          )  
        )
      }

      def incept(ras: DRuleAliases): c.universe.Tree = {
        Apply(  
          Ident(newTermName("DRuleAliases")),
          List(
            Apply(Ident(newTermName("Seq")), ras.aliases.map(incept(_)).toList )
          )
        )
      }

      def incept(ops: Seq[OpParsing]): c.universe.Tree = {        
        Apply(
          Ident(newTermName("Seq")),
          ops.map{
            case add: AddParsing => incept(add)
            case ret: RetractParsing => incept(ret)
            case retEnt: RetractEntityParsing => incept(retEnt)
            case addEnt: AddToEntityParsing => incept(addEnt)
          }.toList
        )       
      }
    }
  } 

  
}