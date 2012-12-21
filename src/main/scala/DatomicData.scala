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
  override def toString = "\""+ underlying + "\""
  def toNative: java.lang.Object = underlying: java.lang.String 
}

case class DBoolean(underlying: Boolean) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Boolean
}

case class DLong(underlying: Long) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Long
}

/** hidden structure just to be able to manipulate Int but should not be used directly by users 
  * and not used in datomic at all 
  */
private[reactivedatomic] case class DInt(underlying: Int) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Integer
}

case class DFloat(underlying: Float) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Float
}

case class DDouble(underlying: Double) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Double
}

case class DBigInt(underlying: BigInt) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying.underlying
}

case class DBigDec(underlying: BigDecimal) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying.underlying
}

case class DInstant(underlying: java.util.Date) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DUuid(underlying: java.util.UUID) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DUri(underlying: java.net.URI) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

case class DBytes(underlying: Array[Byte]) extends DatomicData {
  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying: java.lang.Object
}

case class DRef(underlying: Either[Keyword, DId]) extends DatomicData {
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

  def filter(filterFn: (DDatabase, DDatom) => Boolean): DDatabase = {
    DDatabase(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          val ddb = DDatabase(db)
          filterFn(ddb, DDatom(d)(ddb))
        }
      }
    ))
  }

  def filter(filterFn: DDatom => Boolean): DDatabase = {
    DDatabase(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          filterFn(DDatom(d)(self))
        }
      }
    ))
  }

  def touch(id: Long): Option[DEntity] = touch(DLong(id))
  def touch(id: DLong): Option[DEntity] = entity(id).map( touch(_) )
  def touch(entity: DEntity): DEntity = entity.touch

  def datoms(index: Keyword, components: Keyword*): Seq[DDatom] = {
    //import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._
    underlying.datoms(index.toNative, components.map(_.toNative): _*).toSeq.map( d => DDatom(d)(this) )
  }

  override def toString = underlying.toString
  def toNative: java.lang.Object = underlying
}

object DDatabase {
  def apply(underlying: datomic.Database) = new DDatabase(underlying)

  // Index component contains all datoms
  val EAVT = Keyword("eavt")
  // Index component contains all datoms
  val AEVT = Keyword("aevt")
  // Index component contains datoms for attributes where :db/index = true
  val AVET = Keyword("avet")
  // Index component contains datoms for attributes of :db.type/ref
  val VAET = Keyword("vaet")
} 

trait DId extends DatomicData

case class FinalId(underlying: Long) extends DId {
  //def toNative: java.lang.Object = underlying
  override lazy val toNative: java.lang.Object = underlying: java.lang.Long

  override def toString = toNative.toString
}

case class TempId(partition: Partition, id: Option[Long] = None, dbId: java.lang.Object) extends DId {
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
  def toNative = entity

  def touch() = new DEntity(entity.touch())

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

  /* extension with attributes */
  /*def as[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])
  (implicit dd2dd: DD2DDReader[DD], dd2t: DD2ScalaReader[DD, T]): T = {
    dd2t.read(dd2dd.read(apply(attr.ident)))
  }*/

  def get[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Option[T] = {
    attrC.convert(attr).read(this).toOption
  }

  def tryGet[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Try[T] = {
    attrC.convert(attr).read(this)
  }

  def getRef[T](attr: Attribute[DRef, CardinalityOne.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Option[Ref[T]] = {
    tryGet(attr).toOption
  }

  def getRefs[T](attr: Attribute[DRef, CardinalityMany.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Option[Set[Ref[T]]] = {
    tryGet(attr).toOption
  }

  def tryGetRef[T](attr: Attribute[DRef, CardinalityOne.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Try[Ref[T]] = {
    tryGet(attr)
  }

  def tryGetRefs[T](attr: Attribute[DRef, CardinalityMany.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Try[Set[Ref[T]]] = {
    tryGet(attr)
  }

  def get[T](attr: RefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Option[Ref[T]] = {
    tryGet(attr).toOption
  }

  def tryGet[T](attr: RefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Try[Ref[T]] = {
    attrC.convert(attr).read(this)
  }

  def get[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Option[Set[Ref[T]]] = {
    tryGet(attr).toOption
  }

  def tryGet[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Try[Set[Ref[T]]] = {
    attrC.convert(attr).read(this)
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
  def attrId: DId
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
    lazy val attrId = DId(DLong(d.a.asInstanceOf[java.lang.Integer].toLong))
    lazy val value = DatomicData.toDatomicData(d.v)
    lazy val tx = DLong(d.tx.asInstanceOf[Long])
    lazy val added = DBoolean(d.added.asInstanceOf[Boolean])
    lazy val underlying = d
  }
}

trait DD2ScalaReader[-DD <: DatomicData, A] {
  def read(dd: DD): A
}

object DD2ScalaReader{
  def apply[DD <: DatomicData, A](f: DD => A) = new DD2ScalaReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DD2DDReader[+DD <: DatomicData] {
  def read(d: DatomicData): DD
}

object DD2DDReader{
  def apply[DD <: DatomicData](f: DatomicData => DD) = new DD2DDReader[DD]{
    def read(d: DatomicData): DD = f(d)
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
  implicit val DString2String = DD2ScalaReader{ s: DString => s.underlying }
  implicit val DLong2Long = DD2ScalaReader{ s: DLong => s.underlying }
  implicit val DBoolean2Boolean = DD2ScalaReader{ s: DBoolean => s.underlying }
  implicit val DFloat2Float = DD2ScalaReader{ s: DFloat => s.underlying }
  implicit val DDouble2Double = DD2ScalaReader{ s: DDouble => s.underlying }
  implicit val DBigInt2BigInteger = DD2ScalaReader{ s: DBigInt => s.underlying }
  implicit val DBigDec2BigDecimal = DD2ScalaReader{ s: DBigDec => s.underlying }
  implicit val DInstant2Date = DD2ScalaReader{ s: DInstant => s.underlying }

  implicit val DRef2DRef = DD2ScalaReader{ s: DRef => s }
  implicit val DEntity2DEntity = DD2ScalaReader{ s: DEntity => s }

  //implicit val DString2DString = DD2ScalaReader{ s: DString => s }
  //implicit val DInstant2DInstant = DD2ScalaReader{ s: DInstant => s }
  //implicit def DD2DD[DD <: DatomicData] = DD2ScalaReader{ d: DD => d: DD }

  implicit def DSet2T[DD <: DatomicData, T]
    (implicit dd2dd: DD2DDReader[DD], dd2t: DD2ScalaReader[DD, T]): DD2ScalaReader[DSet, Set[T]] = {
    DD2ScalaReader{ ds: DSet => ds.toSet.map( e => dd2t.read(dd2dd.read(e)) ) }
  }

  implicit val DatomicData2DString: DD2DDReader[DString] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DString => s
    case _ => throw new RuntimeException("expected DString to convert to String")
  }}
  
  implicit val DatomicData2DLong: DD2DDReader[DLong] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s
    case _ => throw new RuntimeException("expected DLong to convert to DLong")
  }}

  implicit val DatomicData2DBoolean: DD2DDReader[DBoolean] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBoolean => s
    case _ => throw new RuntimeException("expected DBoolean to convert to DBoolean")
  }}

  implicit val DatomicData2DFloat: DD2DDReader[DFloat] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DFloat => s
    case _ => throw new RuntimeException("expected DFloat to convert to DFloat")
  }}

  implicit val DatomicData2DDouble: DD2DDReader[DDouble] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DDouble => s
    case _ => throw new RuntimeException("expected DDouble to convert to DDouble")
  }}

  implicit val DatomicData2DBigInt: DD2DDReader[DBigInt] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBigInt => s
    case _ => throw new RuntimeException("expected DBigInt to convert to DBigInt")
  }}

  implicit val DatomicData2DBigDec: DD2DDReader[DBigDec] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBigDec => s
    case _ => throw new RuntimeException("expected DBigDec to convert to DBigDec")
  }}

  implicit val DatomicData2DInstant: DD2DDReader[DInstant] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s
    case _ => throw new RuntimeException("expected DInstant to convert to DInstant")
  }}

  implicit val DatomicData2DEntity: DD2DDReader[DEntity] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}

  implicit val DatomicData2DSet: DD2DDReader[DSet] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  implicit val DatomicData2DRef: DD2DDReader[DRef] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DRef => s
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit def Datomicdata2DD[DD <: DatomicData](implicit dd2dd: DD2DDReader[DD]): DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd2dd.read(dd) }
  /*implicit def genericDDReader[A](implicit dd2t: DD2ScalaReader[DatomicData, A]): DDReader[DatomicData, A] = 
    DDReader{ dd: DatomicData =>
      dd2t.read(dd)
    }*/
  

  implicit val DatomicData2String: DDReader[DatomicData, String] = DDReader{ dd: DatomicData => dd match { 
    case s: DString => s.underlying 
    case _ => throw new RuntimeException("expected DString to convert to String")
  }}

  implicit val DatomicData2Long: DDReader[DatomicData, Long] = DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s.underlying 
    case _ => throw new RuntimeException("expected DLong to convert to Long")
  }}

  implicit val DatomicData2Boolean: DDReader[DatomicData, Boolean] = DDReader{ dd: DatomicData => dd match { 
    case s: DBoolean => s.underlying 
    case _ => throw new RuntimeException("expected DBoolean to convert to Boolean")
  }}

  implicit val DatomicData2Float: DDReader[DatomicData, Float] = DDReader{ dd: DatomicData => dd match { 
    case s: DFloat => s.underlying 
    case _ => throw new RuntimeException("expected DFloat to convert to Float")
  }}

  implicit val DatomicData2Double: DDReader[DatomicData, Double] = DDReader{ dd: DatomicData => dd match { 
    case s: DDouble => s.underlying 
    case _ => throw new RuntimeException("expected DDouble to convert to Double")
  }}

  implicit val DatomicData2BigInt: DDReader[DatomicData, BigInt] = DDReader{ dd: DatomicData => dd match { 
    case s: DBigInt => s.underlying 
    case _ => throw new RuntimeException("expected DBigInt to convert to BigInteger")
  }}

  implicit val DatomicData2BigDec: DDReader[DatomicData, BigDecimal] = DDReader{ dd: DatomicData => dd match { 
    case s: DBigDec => s.underlying 
    case _ => throw new RuntimeException("expected DBigDec to convert to BigDecimal")
  }}

  implicit val DatomicData2Date: DDReader[DatomicData, java.util.Date] = DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s.underlying 
    case _ => throw new RuntimeException("expected DInstant to convert to Data")
  }}

  /*implicit val DRefDDReader: DDReader[DatomicData, DRef] = DDReader{ dd: DatomicData => dd match { 
    case s: DRef => s
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit val DEntityDDReader: DDReader[DatomicData, DEntity] = DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}*/

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] = DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s.toSet.map( reader.read(_) )
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}


  /*implicit def DatomicData2DD[DD <: DatomicData]: DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd match { 
    case s: DD => s
    case _ => throw new RuntimeException("couldn't convert")
  }}*/

  implicit val String2DStringWrites = DDWriter[DString, String]( (s: String) => DString(s) )
  implicit val Long2DLongWrites = DDWriter[DLong, Long]( (l: Long) => DLong(l) )
  implicit val Int2DIntWrites = DDWriter[DInt, Int]( (l: Int) => DInt(l) )
  implicit val Boolean2DBooleanWrites = DDWriter[DBoolean, Boolean]( (b: Boolean) => DBoolean(b) )
  implicit val Float2DFloatWrites = DDWriter[DFloat, Float]( (b: Float) => DFloat(b) )
  implicit val Double2DDoubleWrites = DDWriter[DDouble, Double]( (b: Double) => DDouble(b) )
  implicit val Date2DDateWrites = DDWriter[DInstant, java.util.Date]( (d: java.util.Date) => DInstant(d) )
  implicit val BigInt2DBigIntWrites = DDWriter[DBigInt, java.math.BigInteger]( (i: java.math.BigInteger) => DBigInt(i) )
  implicit val BigDec2DBigDecWrites = DDWriter[DBigDec, java.math.BigDecimal]( (i: java.math.BigDecimal) => DBigDec(i) )
  implicit val Ref2DReferenceable = DDWriter[DRef, Referenceable]( (referenceable: Referenceable) => referenceable.ident )
  implicit val DRef2DRefWrites = DDWriter[DRef, DRef]( (d: DRef) => d )
  //implicit def DDatomicData[DD <: DatomicData] = DDWriter[DD, DD]( dd => dd )
  
  implicit def DD2DStringWrites = DDWriter[DatomicData, DString]{ dd: DatomicData => dd match {
    case d: DString => d
    case _ => throw new RuntimeException("expected DString to convert to DString")
  }}

  implicit def DD2DLongWrites = DDWriter[DatomicData, DLong]{ dd: DatomicData => dd match {
    case d: DLong => d
    case _ => throw new RuntimeException("expected DLong to convert to DLong")
  }}

  implicit def DD2DBooleanWrites = DDWriter[DatomicData, DBoolean]{ dd: DatomicData => dd match {
    case d: DBoolean => d
    case _ => throw new RuntimeException("expected DBoolean to convert to DBoolean")
  }}

  implicit def DD2DFloatWrites = DDWriter[DatomicData, DFloat]{ dd: DatomicData => dd match {
    case d: DFloat => d
    case _ => throw new RuntimeException("expected DFloat to convert to DFloat")
  }}

  implicit def DD2DDoubleWrites = DDWriter[DatomicData, DDouble]{ dd: DatomicData => dd match {
    case d: DDouble => d
    case _ => throw new RuntimeException("expected DDouble to convert to DDouble")
  }}

  implicit def DD2DInstantWrites = DDWriter[DatomicData, DInstant]{ dd: DatomicData => dd match {
    case d: DInstant => d
    case _ => throw new RuntimeException("expected DInstant to convert to DInstant")
  }}

  implicit def DD2DBigIntWrites = DDWriter[DatomicData, DBigInt]{ dd: DatomicData => dd match {
    case d: DBigInt => d
    case _ => throw new RuntimeException("expected DBigInt to convert to DBigInt")
  }}

  implicit def DD2DBigDecWrites = DDWriter[DatomicData, DBigDec]{ dd: DatomicData => dd match {
    case d: DBigDec => d
    case _ => throw new RuntimeException("expected DBigDec to convert to DBigDec")
  }}

  implicit def DD2DRefWrites = DDWriter[DatomicData, DRef]{ dd: DatomicData => dd match {
    case d: DRef => d
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit def DD2DSetWrites = DDWriter[DatomicData, DSet]{ dd: DatomicData => dd match {
    case d: DSet => d
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  implicit def DD2TempIdWrites = DDWriter[DatomicData, TempId]{ dd: DatomicData => dd match {
    case d: TempId => d
    case _ => throw new RuntimeException("expected TempId to convert to TempId")
  }}

  implicit def DD2FinalIdWrites = DDWriter[DatomicData, FinalId]{ dd: DatomicData => dd match {
    case d: FinalId => d
    case _ => throw new RuntimeException("expected FinalId to convert to FinalId")
  }}

  implicit def DD2DEntityWrites = DDWriter[DatomicData, DEntity]{ dd: DatomicData => dd match {
    case d: DEntity => d
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}

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
