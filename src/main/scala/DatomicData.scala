package reactivedatomic

import scala.util.parsing.input.Positional
import scala.util.{Try, Success, Failure}

case class Namespace(name: String) {
  override def toString = name

  def /(name: String) = Keyword(name, Some(this))
}

object Namespace {
  val DB = new Namespace("db") {
    val PART = Namespace("db.part")
    val TYPE = Namespace("db.type")
    val CARDINALITY = Namespace("db.cardinality")
    val INSTALL = Namespace("db.install")
    val UNIQUE = Namespace("db.unique")
    val FN = Namespace("db.fn")
  } 
}

trait Nativeable {
  def toNative: java.lang.Object
}

trait Namespaceable extends Nativeable {
  def name: String
  def ns: Option[Namespace] = None

  override def toString = ":" + ( if(ns.isDefined) {ns.get + "/"} else "" ) + name

  def toNative: java.lang.Object = clojure.lang.Keyword.intern(( if(ns.isDefined) {ns.get + "/"} else "" ) + name )
}

/* DATOMIC TYPES */
sealed trait DatomicData extends Nativeable {
  type DD <: DatomicData

  def as[A](implicit reader: DDReader[DatomicData, A]) = reader.read(this)

  def tryAs[A](implicit reader: DDReader[DatomicData, A]): Try[A] = {
    val t = try {
      Success(this)
    } catch {
      case e: Throwable => Failure(e)
    }

    t.map( reader.read(_) )
  }
}

case class DString(underlying: String) extends DatomicData {
  self =>
  type DD = self.type

  override def toString = "\""+ underlying + "\""
  def toNative: java.lang.Object = underlying: java.lang.String 
}

case class DBoolean(underlying: Boolean) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Boolean
}

case class DLong(underlying: Long) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Long
}

case class DFloat(underlying: Float) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Float
}

case class DDouble(underlying: Double) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Double
}

case class DBigInt(underlying: BigInt) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying.underlying
}

case class DBigDec(underlying: BigDecimal) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying.underlying
}

case class DInstant(underlying: java.util.Date) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DUuid(underlying: java.util.UUID) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DUri(underlying: java.net.URI) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DBytes(underlying: Array[Byte]) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Object
}

case class DRef(underlying: Either[Keyword, DId]) extends DatomicData {
  self =>
  type DD = self.type
  
  override def toString = underlying match {
    case Left(kw) => kw.toString
    case Right(id) => id.toString
  }
  def toNative: java.lang.Object = underlying match {
    case Left(kw) => kw.toNative
    case Right(id) => id.toNative
  }
}

object DRef {
  def apply(kw: Keyword) = new DRef(Left(kw))
  def apply(id: DId) = new DRef(Right(id))
}

class DDatabase(val underlying: datomic.Database) extends DatomicData {
  self =>
  type DD = self.type
  
  def entity(e: DLong): Option[DEntity] = entity(e.underlying)
  def entity(e: Long): Option[DEntity] = 
    Option(underlying.entity(e)).filterNot{ e: datomic.Entity => e.keySet.isEmpty }.map(DEntity(_))
  def entity(e: FinalId): Option[DEntity] = entity(e.underlying)

  def asOf(date: java.util.Date): DDatabase = DDatabase(underlying.asOf(date))
  def asOf(date: DInstant): DDatabase = asOf(date.underlying)

  def since(date: java.util.Date): DDatabase = DDatabase(underlying.since(date))
  def since(date: DInstant): DDatabase = since(date.underlying)

  def entid(e: Long): DId = DId(underlying.entid(e).asInstanceOf[Long])
  def entid(e: DLong): DId = entid(e.underlying)
  def entid(kw: Keyword): DId = DId(underlying.entid(kw.toNative).asInstanceOf[Long])

  def ident(e: Integer): Keyword = Keyword(underlying.ident(e).asInstanceOf[clojure.lang.Keyword])
  def ident(kw: Keyword): Keyword = Keyword(underlying.ident(kw.toNative).asInstanceOf[clojure.lang.Keyword])

  def withData(ops: Seq[Operation]) = {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val javaMap: java.util.Map[_, _] = underlying.`with`(datomicOps)
    
    val m: Map[Any, Any] = javaMap.toMap.map( t => (t._1.toString, t._2) ) 

    val opt = for( 
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]].map( DDatabase(_) ).orElse(None);
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]].map( DDatabase(_) ).orElse(None);
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.Datom]]].orElse(None);
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[Long with datomic.db.DbId, Long]]].orElse(None)
    ) yield TxReport(dbBefore, dbAfter, txData.map(DDatom(_)(this)).toSeq, tempids.toMap)
  
    opt.get
  }

  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

object DDatabase {
  def apply(underlying: datomic.Database) = new DDatabase(underlying)
} 

trait DId extends DatomicData

case class FinalId(underlying: Long) extends DId {
  self =>
  type DD = self.type
  
  //def toNative: java.lang.Object = underlying
  override lazy val toNative: java.lang.Object = underlying: java.lang.Long

  override def toString = toNative.toString
}

case class TempId(partition: Partition, id: Option[Long] = None, dbId: java.lang.Object) extends DId {
  self =>
  type DD = self.type
  
  //def toNative: java.lang.Object = underlying
  override lazy val toNative: java.lang.Object = dbId

  override def toString = toNative.toString
}

object DId {
  def tempid(partition: Partition, id: Option[Long] = None) = id match {
    case None => datomic.Peer.tempid(partition.toString)
    case Some(id) => datomic.Peer.tempid(partition.toString, id)
  }

  def apply(partition: Partition, id: Long) = new TempId(partition, Some(id), DId.tempid(partition, Some(id)))
  def apply(partition: Partition) = new TempId(partition, None, DId.tempid(partition))
  def apply(id: Long) = new FinalId(id)
  def apply(id: DLong) = new FinalId(id.underlying)
  //def apply(partition: Partition = Partition.USER) = new DId(datomic.Peer.tempid(partition.toString).asInstanceOf[datomic.db.DbId])
  //def apply(partition: Partition, id: Long) = new DId(datomic.Peer.tempid(partition.toString, id).asInstanceOf[datomic.db.DbId])

  //def from(dl: DLong)(implicit dd: DDatabase) = dd.entid(dl)
}

/** DSet is a Set but in order to be able to have several tempids in it, this is a seq */
class DSet(elements: Set[DatomicData]) extends DatomicData {
  self =>
  type DD = self.type
  
  def toNative: java.lang.Object = {
    java.util.Arrays.asList(elements.map(_.toNative).toSeq: _*) 
    //new java.util.ArrayList[java.lang.Object]()
    //elements.foreach( e => l.add(e.toNative) )
    
    //import scala.collection.JavaConverters._
    //( elements.map( _.toNative ) ).toList.asJava
  }

  override def toString = elements.mkString("[", ", ", "]")

  def toSet = elements
}

object DSet {
  def apply(set: Set[DatomicData]) = new DSet(set)
  def apply(dd: DatomicData) = new DSet(Set(dd))
  def apply(dd: DatomicData, dds: DatomicData *) = new DSet(Set(dd) ++ dds)
}

class DEntity(val entity: datomic.Entity) extends DatomicData {
  self =>
  type DD = self.type
  
  def toNative = entity

  def apply(keyword: Keyword): DatomicData = DatomicData.toDatomicData( entity.get(keyword.toNative) )

  def get(keyword: Keyword): Option[DatomicData] = {
    if(entity.keySet.isEmpty) None

    Option(entity.get(keyword.toNative)) match {
      case None => None
      case Some(value) => Some(DatomicData.toDatomicData(value))
    }
  }

  def as[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): T = {
    reader.read(apply(keyword))
  } 

  def getAs[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): Option[T] = {
    get(keyword).map( reader.read(_) )
  }

  def tryGet(keyword: Keyword): Try[DatomicData] = {
    if(entity.keySet.isEmpty) Failure(new RuntimeException("empty entity"))

    Option(entity.get(keyword.toNative)) match {
      case None => Failure(new RuntimeException(s"keyword $keyword not found in entity"))
      case Some(value) => Success(DatomicData.toDatomicData(value))
    }
  }

  def tryGetAs[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): Try[T] = {
    tryGet(keyword).map( reader.read(_) )
  }

  def toMap: Map[Keyword, DatomicData] = {
    import scala.collection.JavaConversions._

    entity.keySet.toSet
      .map{x: Any => x.asInstanceOf[clojure.lang.Keyword]}
      .foldLeft(Map[Keyword, DatomicData]()){ (acc, key) => 
        acc + (Keyword(key) -> DatomicData.toDatomicData(entity.get(key)))
      }
  }
}

object DEntity {
  def apply(ent: datomic.Entity) = new DEntity(ent)
}


case class DRuleAlias(name: String, args: Seq[Var], rules: Seq[Rule]) extends DatomicData {
  override def toNative = toString
  override def toString = "[ [%s %s] %s ]".format(
    name, 
    args.map(_.toString).mkString("", " ", ""),
    rules.map(_.toString).mkString("", " ", "")
  )
}

case class DRuleAliases(aliases: Seq[DRuleAlias]) extends DatomicData {
  override def toNative = toString
  override def toString = "[ %s ]".format(
    aliases.map(_.toString).mkString("", " ", "")
  )
}

trait DDatom extends DatomicData{
  def id: DLong
  def attr: Keyword
  def value: DatomicData
  def tx: DLong
  def added: DBoolean
  def underlying: datomic.Datom

  override def toNative = underlying

  override def toString = "[%s %s %s %s %s]".format(id, attr, underlying, tx, added)
}

object DDatom{
  def apply(d: datomic.Datom)(implicit db: DDatabase) = new DDatom{
    lazy val id = DLong(d.e.asInstanceOf[Long])
    lazy val attr = db.ident(d.a.asInstanceOf[java.lang.Integer])
    lazy val value = DatomicData.toDatomicData(d.v)
    lazy val tx = DLong(d.tx.asInstanceOf[Long])
    lazy val added = DBoolean(d.added.asInstanceOf[Boolean])
    lazy val underlying = d
  }
}

trait DDReader[-DD <: DatomicData, +A] {
  def read(dd: DD): A
}

object DDReader{
  def apply[DD <: DatomicData, A](f: DD => A) = new DDReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DDWriter[+DD <: DatomicData, -A] {
  def write(a: A): DD
}

object DDWriter{
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriter[DD, A] {
    def write(a: A) = f(a)
  }
}

trait DWrapper extends NotNull
private[reactivedatomic] case class DWrapperImpl(underlying: DatomicData) extends DWrapper

object DatomicDataImplicits extends DatomicDataImplicits

trait DatomicDataImplicits {
  //implicit val DString2String = DDReader{ s: DString => s.underlying }

  implicit val DatomicData2String: DDReader[DatomicData, String] = DDReader{ dd: DatomicData => dd match { 
    case s: DString => s.underlying 
    case _ => throw new RuntimeException("expected DString to convert to DString")
  }}
  implicit val DatomicData2DString: DDReader[DatomicData, DString] = DDReader{ dd: DatomicData => dd match { 
    case s: DString => s
    case _ => throw new RuntimeException("expected DString to convert to String")
  }}

  //implicit val DLong2Long = DDReader{ s: DLong => s.underlying }

  implicit val DatomicData2Long: DDReader[DatomicData, Long] = DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s.underlying 
    case _ => throw new RuntimeException("expected DLong to convert to Long")
  }}
  implicit val DatomicData2DLong: DDReader[DatomicData, DLong] = DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s
    case _ => throw new RuntimeException("expected DLong to convert to DLong")
  }}

  implicit val DatomicData2Date: DDReader[DatomicData, java.util.Date] = DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s.underlying 
    case _ => throw new RuntimeException("expected DInstant to convert to Data")
  }}
  implicit val DatomicData2DInstant: DDReader[DatomicData, DInstant] = DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s
    case _ => throw new RuntimeException("expected DInstant to convert to DInstant")
  }}

  implicit val DatomicData2DEntity: DDReader[DatomicData, DEntity] = DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}

  implicit val DatomicData2DSet: DDReader[DatomicData, DSet] = DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] = DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s.toSet.map( reader.read(_) )
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  /*implicit def DatomicData2DD[DD <: DatomicData]: DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd match { 
    case s: DD => s
    case _ => throw new RuntimeException("couldn't convert")
  }}*/

  // is this one really reasonable?
  implicit val DLong2Int = DDReader{ s: DLong => s.underlying.toInt }

  implicit val DRef2DRef = DDReader{ s: DRef => s }
  implicit def DD2DD[DD <: DatomicData] = DDReader{ d: DD => d: DD }

  implicit val DStringWrites = DDWriter[DString, String]( (s: String) => DString(s) )
  implicit val Long2DLongWrites = DDWriter[DLong, Long]( (l: Long) => DLong(l) )
  implicit val Int2DLongWrites = DDWriter[DLong, Int]( (l: Int) => DLong(l) )
  implicit val DBooleanWrites = DDWriter[DBoolean, Boolean]( (b: Boolean) => DBoolean(b) )
  implicit val DFloatWrites = DDWriter[DFloat, Float]( (b: Float) => DFloat(b) )
  implicit val DDoubleWrites = DDWriter[DDouble, Double]( (b: Double) => DDouble(b) )
  implicit val DBigIntWrites = DDWriter[DBigInt, java.math.BigInteger]( (i: java.math.BigInteger) => DBigInt(i) )
  implicit val DBigDecWrites = DDWriter[DBigDec, java.math.BigDecimal]( (i: java.math.BigDecimal) => DBigDec(i) )
  implicit val DReferenceable = DDWriter[DRef, Referenceable]( (referenceable: Referenceable) => referenceable.ident )
  implicit def DDatomicData[DD <: DatomicData] = DDWriter[DD, DD]( dd => dd )
  implicit def DRefWrites = DDWriter[DRef, Ref[_]]( (ref: Ref[_]) => DRef(ref.id) )
  implicit def DSetWrites[A](implicit ddw: DDWriter[DatomicData, A]) = 
    DDWriter[DSet, Traversable[A]]{ (l: Traversable[A]) => DSet(l.map{ a => Datomic.toDatomic(a)(ddw) }.toSet) }

}

object DatomicData {

  def toDatomicData(v: Any): DatomicData = v match {
    case s: String => DString(s)
    case b: Boolean => DBoolean(b)
    case i: Int => DLong(i)
    case l: Long => DLong(l)
    case f: Float => DFloat(f)
    case d: Double => DDouble(d)
    case bi: BigInt => DBigInt(bi)
    case bd: BigDecimal => DBigDec(bd)
    case bi: java.math.BigInteger => DBigInt(BigInt(bi))
    case bd: java.math.BigDecimal => DBigDec(BigDecimal(bd))
    case d: java.util.Date => DInstant(d)
    case u: java.util.UUID => DUuid(u)
    case u: java.net.URI => DUri(u)
    case kw: clojure.lang.Keyword => DRef(Keyword(kw.getName, Namespace(kw.getNamespace)))
    case e: datomic.Entity => DEntity(e)
    case coll: java.util.Collection[_] => 
      import scala.collection.JavaConversions._
      DSet(coll.toSet.map{dd: Any => toDatomicData(dd)})

    //DRef(DId(e.get(Keyword("id", Namespace.DB).toNative).asInstanceOf[Long]))
    // REF???
    case v => throw new RuntimeException("Unknown Datomic underlying "+v.getClass)
  }

  import scala.collection.JavaConverters._
  /*def toDatomicNative(d: DatomicData): java.lang.Object = {
    d match {
      case DString(s) => s
      case DBoolean(b) => new java.lang.Boolean(b)
      case DLong(l) => new java.lang.Long(l)
      case DFloat(f) => new java.lang.Float(f)
      case DDouble(d) => new java.lang.Double(d)
      case DDatabase(db) => db
      case DBigDec(bd) => new java.lang.BigDecimal(bd)
      //case d: java.util.Date => DInstant(d)
      //case u: java.util.UUID => DUuid(u)
      //case u: java.net.URI => DUri(u)
      // REF???
      case v => throw new RuntimeException("Can't convert Datomic Data "+ v.toString + " to Native")
    }
  }*/

}


/* DATOMIC TERMS */
sealed trait Term

case class Var(name: String) extends Term {
  override def toString = "?" + name
}

case class Keyword(override val name: String, override val ns: Option[Namespace] = None) extends Term with Namespaceable with Positional

object Keyword {
  def apply(name: String, ns: Namespace) = new Keyword(name, Some(ns))
  def apply(ns: Namespace, name: String) = new Keyword(name, Some(ns))

  def apply(kw: clojure.lang.Keyword) = new Keyword(kw.getName, Some(Namespace(kw.getNamespace)))
}

case class Const(underlying: DatomicData) extends Term {
  override def toString = underlying.toString
}

case object Empty extends Term {
  override def toString = "_"
}

trait DataSource extends Term {
  def name: String

  override def toString = "$" + name
}

case class ExternalDS(override val name: String) extends DataSource
case object ImplicitDS extends DataSource {
  def name = ""
}
