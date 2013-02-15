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

package datomisca

import scala.util.{Try, Success, Failure}

/* DATOMIC TYPES */
trait DatomicData extends Nativeable {
  def as[A](implicit reader: DDReader[DatomicData, A]) = reader.read(this)
}

case class DString(underlying: String) extends DatomicData {
  override def toString = "\""+ underlying + "\""
  def toNative: AnyRef = underlying: java.lang.String
}

case class DBoolean(underlying: Boolean) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Boolean
  def toBoolean = underlying
}

case class DLong(underlying: Long) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Long
  def toLong = underlying
}

/** hidden structure just to be able to manipulate Int but should not be used directly by users 
  * and not used in datomic at all 
  */
private[datomisca] case class DInt(underlying: Int) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Integer
  def toInt = underlying
}

case class DFloat(underlying: Float) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Float
  def toFloat = underlying
}

case class DDouble(underlying: Double) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Double
  def toDouble = underlying
}

case class DBigInt(underlying: BigInt) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying.underlying
  def toBigInt = underlying
}

case class DBigDec(underlying: BigDecimal) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying.underlying
  def toBigDecimal = underlying
}

case class DInstant(underlying: java.util.Date) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toDate = underlying
}

case class DUuid(underlying: java.util.UUID) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toUUID = underlying
}

case class DUri(underlying: java.net.URI) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toURI = underlying
}

case class DBytes(underlying: Array[Byte]) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: AnyRef
  def toBytes = underlying
}

case class DRef(underlying: Either[Keyword, DId]) extends DatomicData {
  override def toString = underlying match {
    case Left(kw) => kw.toString
    case Right(id) => id.toString
  }
  def toNative: AnyRef = underlying match {
    case Left(kw) => kw.toNative
    case Right(id) => id.toNative
  }
}

object DRef {
  def apply(kw: Keyword) = new DRef(Left(kw))
  def apply(id: DId) = new DRef(Right(id))
}

trait DId extends DatomicData

case class FinalId(underlying: Long) extends DId {
  //def toNative: AnyRef = underlying
  override lazy val toNative: AnyRef = underlying: java.lang.Long

  override def toString = toNative.toString
}

case class TempId(partition: Partition, id: Option[Long] = None, dbId: AnyRef) extends DId {
  //def toNative: AnyRef = underlying
  override lazy val toNative: AnyRef = dbId

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
}

/** DSet is a Set but in order to be able to have several tempids in it, this is a seq */
class DSet(elements: Set[DatomicData]) extends DatomicData {
  def toNative: AnyRef = {
    java.util.Arrays.asList(elements.map(_.toNative).toSeq: _*) 
  }

  override def toString = elements.mkString("[", ", ", "]")

  def toSet = elements
}

object DSet {
  def apply(set: Set[DatomicData] = Set()) = new DSet(set)
  def apply(dd: DatomicData) = new DSet(Set(dd))
  def apply(dd: DatomicData, dds: DatomicData *) = new DSet(Set(dd) ++ dds)

  def unapply(dset: DSet): Option[Set[DatomicData]] = Some(dset.toSet)
}

class DDatabase(val underlying: datomic.Database) extends DatomicData {
  self => 

  def entity(e: DLong): DEntity = entity(e.underlying)
  def entity(e: FinalId): DEntity = entity(e.underlying)
  def entity(e: Long): DEntity = Option(underlying.entity(e)) match {
    case None => throw new EntityNotFoundException(DId(e))
    case Some(entity) => 
      if(entity.keySet.isEmpty) throw new EntityNotFoundException(DId(e))
      else DEntity(entity)
  }

  def asOf(date: java.util.Date): DDatabase = DDatabase(underlying.asOf(date))
  def asOf(date: DInstant): DDatabase = asOf(date.underlying)

  def since(date: java.util.Date): DDatabase = DDatabase(underlying.since(date))
  def since(date: DInstant): DDatabase = since(date.underlying)

  def entid(e: Long):     DLong = DLong(underlying.entid(e).asInstanceOf[Long])
  def entid(e: DLong):    DLong = entid(e.underlying)
  def entid(kw: Keyword): DLong = DLong(underlying.entid(kw.toNative).asInstanceOf[Long])

  def ident(e: Integer): Keyword = Keyword(underlying.ident(e).asInstanceOf[clojure.lang.Keyword])
  def ident(kw: Keyword): Keyword = Keyword(underlying.ident(kw.toNative).asInstanceOf[clojure.lang.Keyword])

  def withData(ops: Seq[Operation]) = {
    import scala.collection.JavaConverters._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val javaMap: java.util.Map[_, _] = underlying.`with`(datomicOps)

    Utils.toTxReport(javaMap)(this)
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

  def touch(id: Long): DEntity = touch(DLong(id))
  def touch(id: DLong): DEntity = touch(entity(id))
  def touch(entity: DEntity): DEntity = entity.touch

  def datoms(index: Keyword, components: Keyword*): Seq[DDatom] = {
    //import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._
    underlying.datoms(index.toNative, components.map(_.toNative): _*).toSeq.map( d => DDatom(d)(this) )
  }

  def history = DDatabase(underlying.history)
  

  def id: String = underlying.id
  def isFiltered: Boolean = underlying.isFiltered
  def isHistory: Boolean = underlying.isHistory
  def basisT: Long = underlying.basisT
  def nextT: Long = underlying.nextT
  def sinceT: Option[Long] = Option(underlying.sinceT)

  // TODO
  // indexRange
  // invoke

  override def toString = underlying.toString
  def toNative: AnyRef = underlying
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


class DEntity(val entity: datomic.Entity) extends DatomicData {
  def toNative = entity

  def id: DLong = as[DLong](Namespace.DB / "id")

  def touch() = {
    entity.touch()
    this
  }

  def apply(keyword: Keyword): DatomicData =
    Option {
      entity.get(keyword.toNative)
    } match {
      case None => throw new EntityKeyNotFoundException(keyword)
      case Some(value) => Datomic.toDatomicData(value)
    }

  def get(keyword: Keyword): Option[DatomicData] =
    try {
      Some(apply(keyword))
    } catch {
      case _: EntityKeyNotFoundException => None
    }

  def as[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): T =
    reader.read(apply(keyword))

  
  def getAs[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): Option[T] =
    try {
      Some(as(keyword))
    } catch {
      case _: EntityKeyNotFoundException => None
    }

  def toMap: Map[Keyword, DatomicData] = {
    import scala.collection.JavaConverters._

    entity.keySet.asScala.view map { x: Any =>
      val key = x.asInstanceOf[clojure.lang.Keyword]
      (Keyword(key) -> Datomic.toDatomicData(entity.get(key)))
    } toMap
  }

  /* extension with attributes */
  /*def as[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])
  (implicit dd2dd: DD2DDReader[DD], dd2t: DD2ScalaReader[DD, T]): T = {
    dd2t.read(dd2dd.read(apply(attr.ident)))
  }*/

  def get[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Option[T] = {
    Try { attrC.convert(attr).read(this) }.toOption
  }

  def tryGet[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Try[T] = {
    Try { attrC.convert(attr).read(this) }
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
    Try { attrC.convert(attr).read(this) }
  }

  def get[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Option[Set[Ref[T]]] = {
    tryGet(attr).toOption
  }

  def tryGet[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Try[Set[Ref[T]]] = {
    Try { attrC.convert(attr).read(this) }
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
    lazy val value = Datomic.toDatomicData(d.v)
    lazy val tx = DLong(d.tx.asInstanceOf[Long])
    lazy val added = DBoolean(d.added.asInstanceOf[Boolean])
    lazy val underlying = d
  }
}





