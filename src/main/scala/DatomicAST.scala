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
  def name = "$"
}

/* DATOMIC RULES */
case class Rule(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty) extends Positional
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
  def apply[In <: Args, Out <: Args](q: PureQuery): Query = TypedQuery[In, Out](q)
}

case class PureQuery(override val find: Find, override val in: Option[In] = None, override val where: Where) extends Query

sealed trait Args

case class Args2(_1: DatomicData, _2: DatomicData) extends Args
case class Args3(_1: DatomicData, _2: DatomicData, _3: DatomicData) extends Args

case class TypedQuery[In <: Args, Out <: Args](query: Query) extends Query {
  override def find = query.find
  override def in = query.in
  override def where = query.where
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


  def termSerialize: Term => String = (v: Term) => v match {
    case Var(v) => "?" + v
    case Keyword(kw, ns) => ":" + ( if(ns!="") {ns + "/"} else "" ) + kw
    case Const(c) => datomicDataSerialize(c)
    case ds: DataSource => "$" + ds.name
    case Empty => "_"
  }

  def ruleSerialize: Rule => String = (r: Rule) => 
    "[ " + 
    (if(r.ds == ImplicitDS) "" else (termSerialize(r.ds) + " ") ) + 
    termSerialize(r.entity) + " " + 
    termSerialize(r.attr) + " " + 
    termSerialize(r.value) + 
    " ]"

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
