package reactivedatomic

import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval

object DatomicCompiler {


  def query[A](q: String) = macro queryImpl[A]

  def queryImpl[T: c.AbsTypeTag](c: Context)(q: c.Expr[String]) : c.Expr[Query] = {
      //println(implicitly[c.AbsTypeTag[T]].tpe <:< implicitly[c.TypeTag[Boolean]].tpe)
      //println(c.universe.TypeTag.unapply(implicitly[c.AbsTypeTag[T]].tpe))
        import c.universe.{Literal, Constant, Apply, Ident, reify, newTermName, Expr, Tree, Position}

        def inceptionDD(d: DatomicData): Tree = d match {
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

        def inceptionT(t: Term): Tree = t match {
          case Var(name) => Apply( Ident(newTermName("Var")), List(Literal(Constant(name))) )
          case Keyword(name, ns) => Apply( Ident(newTermName("Var")), List(Literal(Constant(ns + "/" + name))) )
          case Empty => reify("_").tree
          case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(inceptionDD(d)) )
        }

        def inceptionR(r: Rule): Tree = 
          Apply( Ident(newTermName("Rule")), List(Ident(newTermName("ImplicitDS")), inceptionT(r.entity), inceptionT(r.attr), inceptionT(r.value)) )

        def inceptionO(o: Output): Tree = o match {
          case OutVariable(v) => Apply(Ident(newTermName("OutVariable")), List(inceptionT(v)))
        }

        def inceptionW(w: Where): Tree = 
          Apply( Ident(newTermName("Where")), List( Apply(Ident(newTermName("Seq")), w.rules.map(inceptionR(_)).toList )) )
        
        def inceptionF(f: Find): Tree = 
          Apply( Ident(newTermName("Find")), List( Apply(Ident(newTermName("Seq")), f.outputs.map(inceptionO(_)).toList )) )  

        def inception(q: Query): Tree = {
          Apply(
            Ident(newTermName("Query")), 
            List(
              inceptionF(q.find),
              inceptionW(q.where)
            )
          )
        }

        q.tree match {
          case Literal(Constant(s: String)) => 
            //println(DatomicParser.parseQuery2(s).toString)
            //c.Expr[String](c.universe.Literal(c.universe.Constant(DatomicParser.parseQuery2(s).toString)))
            DatomicParser.parseQuery2(s) match {
              case Left(msg) =>
                c.error(c.enclosingPosition.withStart(c.enclosingPosition.line + 5), msg)
                //(c.universe.reify(DatomicParser.parseQuery(s)): c.Expr[Query]).splice
                c.Expr[Query](null)
                //c.universe.reify(msg)
              case Right(q) => c.Expr[Query]( inception(q) )
            }

            //c.Expr[Dummy](Apply(Ident(newTermName("Dummy")), List(reify("toto").tree)))
            //c.Expr[Query]( inception(DatomicParser.parseQuery(s)) )
          case _ => c.Expr[Query](null)
          // c.universe.reify("") //c.universe.reify(DatomicParser.parseQuery("")): c.Expr[Query]
        //case c.universe.Literal(c.universe.Constant("false")) => c.universe.reify(false)
        //case _ => { c.error(c.enclosingPosition,"not a boolean");c.universe.reify(true)}
        }
      
    }
}