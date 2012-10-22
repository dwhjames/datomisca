package reactivedatomic

import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}

object DatomicCompiler {


  def inception(c: Context) = {
    import c.universe.{Literal, Constant, Apply, Ident, reify, newTermName, Expr, Tree, Position}

    new {

      def incept(d: DatomicData): c.Tree = d match {
        case DString(v) => Apply(Ident(newTermName("DString")), List(Literal(Constant("\""+ v + "\""))))
        case DInt(v) => Apply(Ident(newTermName("DInt")), List(Literal(Constant(v))))
        case DLong(v) => Apply(Ident(newTermName("DLong")), List(Literal(Constant(v))))
        case DFloat(v) => Apply(Ident(newTermName("DFloat")), List(Literal(Constant(v))))
        case DDouble(v) => Apply(Ident(newTermName("DDouble")), List(Literal(Constant(v))))
        case DBoolean(v) => Apply(Ident(newTermName("DBoolean")), List(Literal(Constant(v))))
        //case DRef(v) => termSerialize(v)
        //case DBigDec(v) => v.toString
        //case DInstant(v) => v.toString
        //case DUuid(v) => v.toString
        //case DUri(v) => v.toString
        
      }

      def incept(t: Term): c.Tree = t match {
        case Var(name) => Apply( Ident(newTermName("Var")), List(Literal(Constant(name))) )
        case Keyword(name, ns) => Apply( Ident(newTermName("Keyword")), List(Literal(Constant(ns + "/" + name))) )
        case Empty => reify("_").tree
        case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(incept(d)) )
        case ImplicitDS => Ident(newTermName("ImplicitDS"))
        case ExternalDS(n) => Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(n))) ) 
      }

      def incept(df: DFunction) = Apply( Ident(newTermName("DFunction")), List(Literal(Constant(df.name))) )
      def incept(df: DPredicate) = Apply( Ident(newTermName("DPredicate")), List(Literal(Constant(df.name))) )

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
        case DataRule(ds, entity, attr, value) =>
          Apply( Ident(newTermName("DataRule")), 
            List(
              (if(ds == ImplicitDS) Ident(newTermName("ImplicitDS")) else Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(ds.name)))) ), 
              incept(entity), 
              incept(attr), 
              incept(value)
            ) 
          )
        case f: ExpressionRule => 
          Apply( 
            Ident(newTermName("ExpressionRule")), 
            List(incept(f.expr))
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
      }

      def incept(in: In): c.Tree = 
        Apply( Ident(newTermName("In")), List( Apply(Ident(newTermName("Seq")), in.inputs.map(incept(_)).toList )) )

      
      def incept(f: Find): c.Tree = 
        Apply( Ident(newTermName("Find")), List( Apply(Ident(newTermName("Seq")), f.outputs.map(incept(_)).toList )) )  

      def incept(q: Query): c.universe.Tree = {
        Apply(
          Ident(newTermName("Query")), 
          List(incept(q.find)) ++ 
          q.in.map( in => List(incept(in)) ).getOrElse(List()) ++ 
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
    }
  } 

  def pureQuery(q: String) = macro pureQueryImpl

  def pureQueryImpl(c: Context)(q: c.Expr[String]) : c.Expr[Query] = {
    //println(implicitly[c.AbsTypeTag[T]].tpe <:< implicitly[c.TypeTag[Boolean]].tpe)
    //println(c.universe.TypeTag.unapply(implicitly[c.AbsTypeTag[T]].tpe))
      import c.universe._

      val inc = inception(c)

      q.tree match {
        case Literal(Constant(s: String)) => 
          //println(DatomicParser.parseQuery2(s).toString)
          //c.Expr[String](c.universe.Literal(c.universe.Constant(DatomicParser.parseQuery2(s).toString)))
          DatomicParser.parseQuerySafe(s) match {
            case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
              val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset = 
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

//                println("enclosingOffset: %d offsetLine:%d offsetCol:%d".format(enclosingPos.source.lineToOffset(enclosingPos.line), offsetLine, offsetCol))
//                println("line:%d offset:%d".format(enclosingPos.line, enclosingOffset))
              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)
            case Right(q) => c.Expr[Query]( inc.incept(q) )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
        // c.universe.reify("") //c.universe.reify(DatomicParser.parseQuery("")): c.Expr[Query]
      //case c.universe.Literal(c.universe.Constant("false")) => c.universe.reify(false)
      //case _ => { c.error(c.enclosingPosition,"not a boolean");c.universe.reify(true)}
      }
    
  }


  def query[A <: Args, B <: Args](q: String) = macro queryImpl[A, B]

  def queryImpl[A <: Args : c.AbsTypeTag, B <: Args : c.AbsTypeTag](c: Context)(q: c.Expr[String]) : c.Expr[TypedQuery[A, B]] = {
      //println(implicitly[c.AbsTypeTag[T]].tpe <:< implicitly[c.TypeTag[Boolean]].tpe)
      //println(c.universe.TypeTag.unapply(implicitly[c.AbsTypeTag[T]].tpe))

      def verifyInputs(query: Query): Either[PositionFailure, TypedQuery[A, B]] = {
        val tpe = implicitly[c.AbsTypeTag[A]].tpe
        val sz = query.in.map( _.inputs.size ).getOrElse(0)
        lazy val argPos = c.macroApplication.children(0).children(1).pos

        query.in.map{ in => 
          if(
            (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2) 
            || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
          )
            Left(PositionFailure("Query Error in \":in\" : Expected %d INPUT variables".format(sz), 1, argPos.column))
          else Right(TypedQuery[A,B](query))
        } 
        .getOrElse(Right(TypedQuery[A,B](query)))
        
      }

      def verifyOutputs(query: Query): Either[PositionFailure, TypedQuery[A, B]] = {
        val tpe = implicitly[c.AbsTypeTag[B]].tpe
        val sz = query.find.outputs.size
        val argPos = c.macroApplication.children(0).children(2).pos

        if(
          (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2)
          || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
        )
          Left(PositionFailure("Query Error in \":find\" : Expected %d OUTPUT variables".format(sz), 1, argPos.column))
        else Right(TypedQuery[A,B](query))
      }

      def verifyTypes(query: Query): Either[PositionFailure, TypedQuery[A, B]] = {
        verifyInputs(query).right.flatMap( _ => 
          verifyOutputs(query) 
        )
      }

      import c.universe._

      val inc = inception(c)

      q.tree match {
        case Literal(Constant(s: String)) => 
          //println(DatomicParser.parseQuery2(s).toString)
          //c.Expr[String](c.universe.Literal(c.universe.Constant(DatomicParser.parseQuery2(s).toString)))
          DatomicParser.parseQuerySafe(s).right.flatMap( (t: Query) => verifyTypes(t) ) match {
            case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
              val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset = 
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)

            case Right(tq) => 
              c.Expr[TypedQuery[A, B]]( inc.incept(tq) )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
        // c.universe.reify("") //c.universe.reify(DatomicParser.parseQuery("")): c.Expr[Query]
      //case c.universe.Literal(c.universe.Constant("false")) => c.universe.reify(false)
      //case _ => { c.error(c.enclosingPosition,"not a boolean");c.universe.reify(true)}
      }
      
    }
}