package reactivedatomic

import scala.util.{Try, Success, Failure}
import scala.util.parsing.input.Positional

/* DATOMIC TYPES */
sealed trait DatomicData
case class DString(value: String) extends DatomicData 
case class DBoolean(value: Boolean) extends DatomicData
case class DInt(value: Int) extends DatomicData
case class DLong(value: Long) extends DatomicData
case class DFloat(value: Float) extends DatomicData
case class DDouble(value: Double) extends DatomicData
case class DBigDec(value: BigDecimal) extends DatomicData
case class DInstant(value: java.util.Date) extends DatomicData
case class DUuid(value: java.util.UUID) extends DatomicData
case class DUri(value: java.net.URI) extends DatomicData
case class DRef(value: Keyword) extends DatomicData
case class DDatabase(value: datomic.Database) extends DatomicData {
  def entity(e: DLong) = value.entity(e.value)
}

object DatomicData {

  def toDatomicData(v: Any): DatomicData = v match {
    case s: String => DString(s)
    case b: Boolean => DBoolean(b)
    case i: Int => DInt(i)
    case l: Long => DLong(l)
    case f: Float => DFloat(f)
    case d: Double => DDouble(d)
    case bd: BigDecimal => DBigDec(bd)
    case d: java.util.Date => DInstant(d)
    case u: java.util.UUID => DUuid(u)
    case u: java.net.URI => DUri(u)
    // REF???
    case _ => throw new RuntimeException("Unknown Datomic Value")
  }

  import scala.collection.JavaConverters._
  def toDatomicNative(d: DatomicData): java.lang.Object = {
    d match {
      case DString(s) => s
      case DBoolean(b) => new java.lang.Boolean(b)
      case DInt(i) => new java.lang.Integer(i)
      case DLong(l) => new java.lang.Long(l)
      case DFloat(f) => new java.lang.Float(f)
      case DDouble(d) => new java.lang.Double(d)
      case DDatabase(db) => db
      //case DBigDec(bd) => new java.lang.BigDecimal(bd)
      //case d: java.util.Date => DInstant(d)
      //case u: java.util.UUID => DUuid(u)
      //case u: java.net.URI => DUri(u)
      // REF???
      case _ => throw new RuntimeException("Can't convert Datomic Data to Native")
    }
  }

}


/* DATOMIC TERMS */
sealed trait Term

case class Var(name: String) extends Term

case class Keyword(name: String, ns: String = "") extends Term
case class Const(value: DatomicData) extends Term
case object Empty extends Term

trait DataSource extends Term {
  def name: String
}
case class ExternalDS(override val name: String) extends DataSource
case object ImplicitDS extends DataSource {
  def name = ""
}

/* DATOMIC RULES */
sealed trait Rule extends Positional

/* DATOMIC DATA RULES */
case class DataRule(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty) extends Rule

/* DATOMIC EXPRESSION RULES */
sealed trait Binding
case class ScalarBinding(name: Var) extends Binding
case class TupleBinding(names: Seq[Var]) extends Binding
case class CollectionBinding(name: Var) extends Binding
case class RelationBinding(names: Seq[Var]) extends Binding

case class DFunction(name: String)
case class DPredicate(name: String)

case class ExpressionRule(expr: Expression) extends Rule

sealed trait Expression
case class PredicateExpression(predicate: DPredicate, args: Seq[Term]) extends Expression
case class FunctionExpression(function: DFunction, args: Seq[Term], binding: Binding) extends Expression

case class Where(rules: Seq[Rule]) extends Positional

/* DATOMIC INPUTS */
case class In(inputs: Seq[Input]) extends Positional

sealed trait Input
case class InDataSource(ds: DataSource) extends Input
case class InVariable(variable: Var) extends Input

/* DATOMIC OUTPUTS */
case class Find(outputs: Seq[Output]) extends Positional

sealed trait Output
case class OutVariable(variable: Var) extends Output

/* DATOMIC QUERY */
trait Query {
  def find: Find
  def in: Option[In] = None
  def where: Where
}

object Query {
  def apply(find: Find, where: Where): Query = PureQuery(find, None, where)
  def apply(find: Find, in: In, where: Where): Query = PureQuery(find, Some(in), where)
  def apply(find: Find, in: Option[In], where: Where): Query = PureQuery(find, in, where)
  //def apply[In <: Args, Out <: Args](q: PureQuery): Query = TypedQuery[In, Out](q)
}

case class PureQuery(override val find: Find, override val in: Option[In] = None, override val where: Where) extends Query

sealed trait Args {
  def toSeq: Seq[Object]
}

case class Args0() extends Args {
  override def toSeq: Seq[Object] = Seq()
}
case class Args2(_1: DatomicData, _2: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(DatomicData.toDatomicNative(_1), DatomicData.toDatomicNative(_2))
}
case class Args3(_1: DatomicData, _2: DatomicData, _3: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(DatomicData.toDatomicNative(_1), DatomicData.toDatomicNative(_2), DatomicData.toDatomicNative(_3))
}

/**
 * Converts a function In => T into another function 
 * that returns Out but takes other input parameters 
 * built from In
 */
trait ToFunction[In <: Args, Out] {
  type F[Out]
  def convert(from: (In => Out)): F[Out]
}

/**
 * Convert Args into a Tuple
 */
trait ArgsToTuple[A <: Args, T] {
  def convert(from: A): T
}

/**
 * Converts a Seq[DatomicData] into an Args with potential error (exception)
 */
trait DatomicDataToArgs[T <: Args] {
  def toArgs(l: Seq[DatomicData]): Try[T]
}

trait DatomicExecutor {
  type F[_]
  def execute: F[_]
}

trait ArgsImplicits {

  implicit def toF0[Out] = new ToFunction[Args0, Out] {
    type F[Out] = Function0[Out]
    def convert(f: (Args0 => Out)): F[Out] = () => f(Args0())
  }

  implicit def toF2[Out] = new ToFunction[Args2, Out] {
    type F[Out] = Function2[DatomicData, DatomicData, Out]
    def convert(f: (Args2 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData) => f(Args2(d1, d2)) 
  }

  implicit def toF3[Out] = new ToFunction[Args3, Out] {
    type F[Out] = Function3[DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args3 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData) => f(Args3(d1, d2, d3))
  }

  implicit object DatomicDataToArgs2 extends DatomicDataToArgs[Args2] {
    def toArgs(l: Seq[DatomicData]): Try[Args2] = l match {
      case List(_1, _2) => Success(Args2(_1, _2))
      case _ => Failure(new RuntimeException("Could convert Seq to Args2"))
    }
  }

  implicit def DatomicDataToArgs3 = new DatomicDataToArgs[Args3] {
    def toArgs(l: Seq[DatomicData]): Try[Args3] = l match {
      case List(_1, _2, _3) => Success(Args3(_1, _2, _3))
      case _ => Failure(new RuntimeException("Could convert Seq to Args3"))
    }
  }

  implicit def Args2ToTuple = new ArgsToTuple[Args2, (DatomicData, DatomicData)] {
    def convert(from: Args2) = (from._1, from._2)
  }

  implicit def Args3ToTuple = new ArgsToTuple[Args3, (DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args3) = (from._1, from._2, from._3)
  }

}

case class TypedQuery[In <: Args, Out <: Args](query: Query) extends Query {
  override def find = query.find
  override def in = query.in
  override def where = query.where

  def apply(in: In)(implicit db: DDatabase, outConv: DatomicDataToArgs[Out]): Try[List[Out]] = directQuery(in)

  def directQuery(in: In)(implicit db: DDatabase, outConv: DatomicDataToArgs[Out]): Try[List[Out]] = {
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._
    val qser = DatomicSerializers.querySerialize(query)
    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(DatomicData.toDatomicNative(db): Object)
      else args
    }

    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    val listOfTry = results.map { fields =>
      outConv.toArgs(fields.map { field => DatomicData.toDatomicData(field) })
    }

    listOfTry.foldLeft(Success(Nil): Try[List[Out]]){ (acc, e) => e match {
      case Success(t) => acc.map( (a: List[Out]) => a :+ t )
      case Failure(f) => Failure(f)
    } }
  }

  def prepare[T](
    implicit db: DDatabase, outConv: DatomicDataToArgs[Out], ott: ArgsToTuple[Out, T], 
             tf: ToFunction[In, Try[List[T]]]
  ) = new DatomicExecutor {
    type F[_] = tf.F[Try[List[T]]]
    def execute = tf.convert(
      (directQuery _).andThen((t: Try[List[Out]]) => t.map( _.map( out => ott.convert(out)) ))
    )
  }

}

object DatomicSerializers extends DatomicSerializers

trait DatomicSerializers {
  def datomicDataSerialize: DatomicData => String = (d: DatomicData) => d match {
    case DString(v) => "\""+ v + "\""
    case DInt(v) => v.toString
    case DLong(v) => v.toString
    case DFloat(v) => v.toString
    case DDouble(v) => v.toString
    case DRef(v) => termSerialize(v)
    case DBigDec(v) => v.toString
    case DInstant(v) => v.toString
    case DUuid(v) => v.toString
    case DUri(v) => v.toString
    case DBoolean(v) => v.toString
  }

  def datomicFunctionSerialize: DFunction => String = (d: DFunction) => d.name
  def datomicPredicateSerialize: DPredicate => String = (d: DPredicate) => d.name

  def termSerialize: Term => String = (v: Term) => v match {
    case Var(v) => "?" + v
    case Keyword(kw, ns) => ":" + ( if(ns!="") {ns + "/"} else "" ) + kw
    case Const(c) => datomicDataSerialize(c)
    case ds: DataSource => "$" + ds.name
    case Empty => "_"
  }

  def bindingSerialize: Binding => String = (v: Binding) => v match {
    case ScalarBinding(name) => termSerialize(name)
    case TupleBinding(names) => "[ " + names.map( termSerialize(_) ).mkString(" ") + " ]"
    case CollectionBinding(name) => "[ " + termSerialize(name) + " ... ]" 
    case RelationBinding(names) => "[[ " + names.map( termSerialize(_)).mkString(" ") + " ]]"
  }

  def dataRuleSerialize: DataRule => String = (r: DataRule) => 
    (if(r.ds == ImplicitDS) "" else (termSerialize(r.ds) + " ") ) + 
    termSerialize(r.entity) + " " + 
    termSerialize(r.attr) + " " + 
    termSerialize(r.value)


  def predicateExpressionSerialize: PredicateExpression => String = (r: PredicateExpression) =>
    "(" +
      datomicPredicateSerialize(r.predicate) + " " +
      r.args.map( termSerialize(_) ).mkString(" ") +
    ")"

  def functionExpressionSerialize: FunctionExpression => String = (r: FunctionExpression) =>
    "(" +
      datomicFunctionSerialize(r.function) + " " +
      r.args.map( termSerialize(_) ).mkString(" ") +
    ") " + 
      bindingSerialize(r.binding)

  def expressionSerialize: Expression => String = (r: Expression) => r match {
    case p: PredicateExpression => predicateExpressionSerialize(p)
    case f: FunctionExpression => functionExpressionSerialize(f)
  }

  def ruleSerialize: Rule => String = (r: Rule) => "[ " + (r match {
    case p: DataRule => dataRuleSerialize(p)
    case ExpressionRule(expr) => expressionSerialize(expr)
  }) + " ]"

  def whereSerialize: Where => String = (w: Where) => 
    w.rules.map( ruleSerialize(_) ).mkString(":where ", " ", "")

  def outputSerialize: Output => String = (o: Output) => o match {
    case OutVariable(v) => termSerialize(v)
  }

  def findSerialize: Find => String = (f: Find) =>
    f.outputs.map( outputSerialize(_) ).mkString(":find ", " ", "")

  def inputSerialize: Input => String = (i: Input) => i match {
    case InVariable(v) => termSerialize(v)
    case InDataSource(ds) => termSerialize(ds)
  }

  def inSerialize: In => String = (i: In) =>
    i.inputs.map( inputSerialize(_) ).mkString(":in ", " ", "")

  def querySerialize: Query => String = (q: Query) =>
    "[ " + 
      findSerialize(q.find) + " " + 
      q.in.map( inSerialize(_) + " " ).getOrElse("") + 
      whereSerialize(q.where) + 
    " ]"

}
