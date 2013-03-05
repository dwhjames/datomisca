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

  def asEither = underlying

  def toId = underlying match {
    case Right(id) => id
    case _         => throw new DatomicException("DRef was not an Id but a Keyword")
  }

  def toKeyword = underlying match {
    case Left(kw)  => kw
    case _         => throw new DatomicException("DRef was not an Keyword but an DId") 
  }
}

object DRef {
  def apply(kw: Keyword) = new DRef(Left(kw))
  def apply(id: DId)     = new DRef(Right(id))
  def apply(id: DLong)   = new DRef(Right(DId(id)))
  def apply(id: Long)    = new DRef(Right(DId(id)))

  object IsKeyword {
    def unapply(ref: DRef): Option[Keyword] = ref.underlying match {
      case Left(kw) => Some(kw)
      case _         => None
    }
  }

  object IsId {
    def unapply(ref: DRef): Option[DId] = ref.underlying match {
      case Right(id) => Some(id)
      case _         => None
    }
  }
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
  val id:         Long
  val attr:       Keyword
  val attrId:     Long
  val value:      DatomicData
  val tx:         Long
  val added:      Boolean
  val underlying: datomic.Datom

  override def toNative = underlying

  override def toString = "Datom(e[%s] a[%s] v[%s] tx[%s] added[%s])".format(id, attr, value, tx, added)
}

object DDatom{
  def apply(d: datomic.Datom)(implicit db: DDatabase) = new DDatom{
    lazy val id = d.e.asInstanceOf[Long]
    lazy val attr = db.ident(d.a.asInstanceOf[Integer].toLong)
    lazy val attrId = d.a.asInstanceOf[Integer].toLong
    lazy val value = Datomic.toDatomicData(d.v)
    lazy val tx = d.tx.asInstanceOf[Long]
    lazy val added = d.added.asInstanceOf[Boolean]
    lazy val underlying = d
  }
}





