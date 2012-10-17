package reactivedatomic

import scala.util.{Try, Success, Failure}


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
case class Rule(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty)
case class Where(rules: Seq[Rule])

/* DATOMIC INPUTS */
case class In(inputs: Seq[Input])

sealed trait Input
case class InDataSource(ds: DataSource) extends Input
case class InVariable(variable: Var) extends Input

/* DATOMIC OUTPUTS */
case class Find(outputs: Seq[Output])

sealed trait Output
case class OutVariable(variable: Var) extends Output

/* DATOMIC QUERY */
case class Query(find: Find, in: Option[In] = None, where: Where)

case class Dummy(s: String)

object Query {
  def apply(find: Find, where: Where) = new Query(find, None, where)
  def apply(find: Find, in: In, where: Where) = new Query(find, Some(in), where)
}

trait DatomicDataListConverter[T] {
  def convert(d: List[DatomicData]): Try[T]
}

trait DatomicDataConverter[T] {
  def convert(d: DatomicData): Try[T]
}

object DatomicData {
  def as[T](list: List[DatomicData])(implicit dc: DatomicDataListConverter[T]): Try[T] = dc.convert(list)
  def as[T](d: DatomicData)(implicit dc: DatomicDataConverter[T]): Try[T] = dc.convert(d)

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

  implicit object toDString extends DatomicDataConverter[DString] {
    def convert(d: DatomicData): Try[DString] = d match {
      case d: DString => Success(d)
      case _ => Failure(new RuntimeException("expected DString found %s".format(d)))
    }
  }

  implicit object toDLong extends DatomicDataConverter[DLong] {
    def convert(d: DatomicData): Try[DLong] = d match {
      case l: DLong => Success(l)
      case _ => Failure(new RuntimeException("expected DLong found %s".format(d)))
    }
  }

  implicit object toDInt extends DatomicDataConverter[DInt] {
    def convert(d: DatomicData): Try[DInt] = d match {
      case l: DInt => Success(l)
      case _ => Failure(new RuntimeException("expected DInt found %s".format(d)))
    }
  }

  implicit object toDBoolean extends DatomicDataConverter[DBoolean] {
    def convert(d: DatomicData): Try[DBoolean] = d match {
      case l: DBoolean => Success(l)
      case _ => Failure(new RuntimeException("expected DBoolean found %s".format(d)))
    }
  }

  implicit object toDFloat extends DatomicDataConverter[DFloat] {
    def convert(d: DatomicData): Try[DFloat] = d match {
      case l: DFloat => Success(l)
      case _ => Failure(new RuntimeException("expected DFloat found %s".format(d)))
    }
  }

  implicit object toDDouble extends DatomicDataConverter[DDouble] {
    def convert(d: DatomicData): Try[DDouble] = d match {
      case l: DDouble => Success(l)
      case _ => Failure(new RuntimeException("expected DDouble found %s".format(d)))
    }
  }

  implicit def toDatomicTuple2[A, B](implicit ca: DatomicDataConverter[A], cb: DatomicDataConverter[B]) = 
    new DatomicDataListConverter[(A, B)] {
      def convert(l: List[DatomicData]) = l match {
        case List(a, b) => as[A](a).flatMap( a => as[B](b).map( b => (a, b) ) )
        case _ => Failure(new RuntimeException("expected Tuple2 found %s".format(l)))
      }
    }

  implicit def toDatomicTuple3[A, B, C](implicit ca: DatomicDataConverter[A], cb: DatomicDataConverter[B], cc: DatomicDataConverter[C]) = 
    new DatomicDataListConverter[(A, B, C)] {
      def convert(l: List[DatomicData]) = l match {
        case List(a, b, c) => as[A](a).flatMap( a => as[B](b).flatMap( b => as[C](c).map( c => (a, b, c) ) ) )
        case _ => Failure(new RuntimeException("expected Tuple3 found %s".format(l)))
      }
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
