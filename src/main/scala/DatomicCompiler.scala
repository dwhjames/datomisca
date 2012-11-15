package reactivedatomic

import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}

trait DatomicCompiler {
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

      def incept(d: DatomicData): c.Tree = d match {
        case DString(v) => Apply(Ident(newTermName("DString")), List(Literal(Constant(v))))
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
        case Empty => reify("_").tree
        case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(incept(d)) )
        case ds: DataSource => incept(ds)
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

      def incept(q: PureQuery): c.universe.Tree = {
        Apply(
          Ident(newTermName("PureQuery")), 
          List(incept(q.find)) ++ 
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
        //val tpe = compiled.tpe
        //if(implicitly[c.TypeTag[Referenceable]].tpe <:< implicitly[c.TypeTag[Referenceable]].tpe) println("TOTO")
        // manage case where this is not a DatomicData
        /*c.universe.reify(compiled) match {
          case id : Ident => 
            println("TYPE:"+id.tpe)
            if(id.tpe <:< typeOf[Referenceable]) println("YO")
            Apply(Ident(newTermName("DString")), List(Select(id, "toString")))
          case t => println("TTTTT:"+t); compiled
        }*/
        Apply(Select(Ident(newTermName("Datomic")), "toDWrapper"), List(compiled))
        //compiled
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

      def incept(a: AddEntityParsing): c.universe.Tree = {
        def inceptId(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
          case Left(se: ScalaExpr) => 
            c.parse(se.expr)
          case _ => c.abort(c.enclosingPosition, ":db/id can only be a DId")
        }

        def localIncept(v: Either[ParsingExpr, DatomicData]): c.universe.Tree = v match {
          case Left(se: ScalaExpr) => incept(se)
          case Left(se: DSetParsing) => incept(se)
          case Right(dd: DatomicData) => incept(dd)
        }

        if(!a.props.contains(Keyword("id", Namespace.DB)))
          c.abort(c.enclosingPosition, "An AddEntity requires one :db/id field")
        else {
          val tree = Apply(
            Apply(
              Select(Ident(newTermName("Datomic")), "addEntity"),
              List(inceptId(a.props(Keyword("id", Namespace.DB))))
            ),
            ( a.props - Keyword("id", Namespace.DB) ).map{ case (k, v) => 
              Apply(Ident(newTermName("Tuple2")), List( incept(k), localIncept(v) ))
            }.toList
          )
          tree
        }
      }
    }
  } 

  
}