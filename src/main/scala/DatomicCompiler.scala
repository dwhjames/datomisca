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
        case DString(v) => Literal(Constant("\""+ v + "\""))
        case DInt(v) => Literal(Constant(v))
        case DLong(v) => Literal(Constant(v))
        case DFloat(v) => Literal(Constant(v))
        case DDouble(v) => Literal(Constant(v))
        case DBoolean(v) => Literal(Constant(v))
        //case DRef(v) => termSerialize(v)
        //case DBigDec(v) => v.toString
        //case DInstant(v) => v.toString
        //case DUuid(v) => v.toString
        //case DUri(v) => v.toString
        
      }

      def incept(t: Term): c.Tree = t match {
        case Var(name) => Apply( Ident(newTermName("Var")), List(Literal(Constant(name))) )
        case Keyword(name, ns) => Apply( Ident(newTermName("Var")), List(Literal(Constant(ns + "/" + name))) )
        case Empty => reify("_").tree
        case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(incept(d)) )
      }

      def incept(r: Rule): c.Tree = 
        Apply( Ident(newTermName("Rule")), List(Ident(newTermName("ImplicitDS")), incept(r.entity), incept(r.attr), incept(r.value)) )

      def incept(o: Output): c.Tree = o match {
        case OutVariable(v) => Apply(Ident(newTermName("OutVariable")), List(incept(v)))
      }

      def incept(w: Where): c.Tree = 
        Apply( Ident(newTermName("Where")), List( Apply(Ident(newTermName("Seq")), w.rules.map(incept(_)).toList )) )
      
      def incept(f: Find): c.Tree = 
        Apply( Ident(newTermName("Find")), List( Apply(Ident(newTermName("Seq")), f.outputs.map(incept(_)).toList )) )  

      def incept(q: Query): c.universe.Tree = {
        Apply(
          Ident(newTermName("Query")), 
          List(
            incept(q.find),
            incept(q.where)
          )
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

      def verifyTyped[T](typeTag: c.AbsTypeTag[T], query: Query): Either[PositionFailure, TypedQuery[A, B]] = {
        val tpe = typeTag.tpe

        if(tpe <:< implicitly[c.TypeTag[Args2]].tpe && query.find.outputs.size != 2) {
          val argPos = c.macroApplication.children(0).children(2).pos
          Left(PositionFailure("Expected 2 ouput variables in the query", 1, argPos.column))
        }
        else if(tpe <:< implicitly[c.TypeTag[Args3]].tpe && query.find.outputs.size != 3) {
          val argPos = c.macroApplication.children(0).children(2).pos
          Left(PositionFailure("Expected 3 ouput variables in the query", 1, argPos.column))
        }
        else Right(TypedQuery[A,B](query))
      }

      def verifyTypes(query: Query): Either[PositionFailure, TypedQuery[A, B]] = {
        verifyTyped[A](implicitly[c.AbsTypeTag[A]], query).right
          .flatMap( _ => verifyTyped[B](implicitly[c.AbsTypeTag[B]], query) )
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