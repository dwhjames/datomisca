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


/* DATOMIC TYPES */
trait DatomicData extends Nativeable {
  def as[A](implicit fdat: FromDatomicCast[A]) = fdat.from(this)
}

private[datomisca] object DatomicData {

  /** Converts any data to a Datomic Data (or not if not possible) */
  private[datomisca] def toDatomicData(v: AnyRef): DatomicData = v match {
    // :db.type/string
    case s: java.lang.String => DString(s)
    // :db.type/boolean
    case b: java.lang.Boolean => DBoolean(b)
    // :db.type/long
    case l: java.lang.Long => DLong(l)
    // attribute id
    case i: java.lang.Integer => DLong(i.toLong)
    // :db.type/float
    case f: java.lang.Float => DFloat(f)
    // :db.type/double
    case d: java.lang.Double => DDouble(d)
    // :db.type/bigint
    case bi: java.math.BigInteger => DBigInt(BigInt(bi))
    // :db.type/bigdec
    case bd: java.math.BigDecimal => DBigDec(BigDecimal(bd))
    // :db.type/instant
    case d: java.util.Date => DInstant(d)
    // :db.type/uuid
    case u: java.util.UUID => DUuid(u)
    // :db.type/uri
    case u: java.net.URI => DUri(u)
    // :db.type/keyword
    case kw: clojure.lang.Keyword => 
      DKeyword(Keyword(kw.getName, Option(kw.getNamespace).map(Namespace(_))))
    // :db.type/bytes
    case bytes: Array[Byte] => DBytes(bytes)
    // an entity map
    case e: datomic.Entity => DEntity(e)
    // a collection
    case coll: java.util.Collection[_] =>
      new DColl(new Iterable[DatomicData] {
        override def iterator = new Iterator[DatomicData] {
          private val jIter = coll.iterator.asInstanceOf[java.util.Iterator[AnyRef]]
          override def hasNext = jIter.hasNext
          override def next(): DatomicData = toDatomicData(jIter.next())
        }
      })
    // otherwise
    case v => throw new UnexpectedDatomicTypeException(v.getClass.getName)
  }
}

final case class DString(underlying: String) extends DatomicData {
  override def toString = "\""+ underlying + "\""
  def toNative: AnyRef = underlying: java.lang.String
}

final case class DBoolean(underlying: Boolean) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Boolean
  def toBoolean = underlying
}

final case class DLong(underlying: Long) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Long
  def toLong = underlying
}

final case class DFloat(underlying: Float) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Float
  def toFloat = underlying
}

final case class DDouble(underlying: Double) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: java.lang.Double
  def toDouble = underlying
}

final case class DBigInt(underlying: BigInt) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying.underlying
  def toBigInt = underlying
}

final case class DBigDec(underlying: BigDecimal) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying.underlying
  def toBigDecimal = underlying
}

final case class DInstant(underlying: java.util.Date) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toDate = underlying
}

final case class DUuid(underlying: java.util.UUID) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toUUID = underlying
}

final case class DUri(underlying: java.net.URI) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying
  def toURI = underlying
}

final case class DBytes(underlying: Array[Byte]) extends DatomicData {
  override def toString = underlying.toString
  def toNative: AnyRef = underlying: AnyRef
  def toBytes = underlying
}

final case class DKeyword(underlying: Keyword) extends DatomicData {
  override def toString = underlying.toString
  override def toNative = underlying.toNative
  def toKeyword = underlying
}

final case class DRef(underlying: Either[Keyword, DId]) extends DatomicData {
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
  def apply[T](t: T)(implicit ev: ToDRef[T]) = ev.toDRef(t)

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

sealed trait ToDRef[T] {
  def toDRef(t: T): DRef
}

object ToDRef {

  implicit val long2DRef:    ToDRef[Long]    = new ToDRef[Long]    { def toDRef(l:  Long)    = new DRef(Right(DId(l))) }
  implicit val dlong2DRef:   ToDRef[DLong]   = new ToDRef[DLong]   { def toDRef(l:  DLong)   = new DRef(Right(DId(l))) }

  implicit def did2DRef[I <: DId]: ToDRef[I] = new ToDRef[I]       { def toDRef(i:  I)       = new DRef(Right(i)) }

  implicit def keyword2DRef[K](implicit toKeyword: K => Keyword): ToDRef[K] =
    new ToDRef[K] {
      def toDRef(k: K) = new DRef(Left(toKeyword(k)))
    }
}

sealed trait DId extends DatomicData

final case class FinalId(underlying: Long) extends DId {
  //def toNative: AnyRef = underlying
  override lazy val toNative: AnyRef = underlying: java.lang.Long

  override def toString = toNative.toString
}

final case class TempId(partition: Partition, id: Option[Long] = None, dbId: AnyRef) extends DId {
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
  def apply[T](id: T)(implicit ev: AsEntityId[T]) = ev.conv(id)
}

/**
  * A conversion type class for entity ids.
  *
  * A type class for converting the various types that can be treated
  * as temporary or permanent ids for entities.
  *
  * @tparam T
  *     the type of the id to convert.
  */
sealed trait AsEntityId[T] {

  /**
    * Convert to an entity id.
    *
    * @param t
    *     an id value to convert.
    * @return the abstracted id.
    */
  protected[datomisca] def conv(t: T): DId
}

/**
  * The three cases for converting entity ids.
  */
object AsEntityId {

  /** Any type viewable as a Long can be an entity id. */
  implicit def long[L](implicit toLong: L => Long): AsEntityId[L] =
    new AsEntityId[L] {
      override protected[datomisca] def conv(l: L) = new FinalId(toLong(l))
    }

  /** A [[DLong]] can be an entity id. */
  implicit val dlong: AsEntityId[DLong] =
    new AsEntityId[DLong] {
      override protected[datomisca] def conv(l: DLong) = new FinalId(l.underlying)
    }

  /** Any subtype of [[DId]] can be a permament entity id. */
  implicit def dId[I <: DId]: AsEntityId[I] =
    new AsEntityId[I] {
      override protected[datomisca] def conv(i: I) = i
    }
}

/**
  * A conversion type class for permanent entity ids.
  *
  * A type class for converting from the various types
  * that can be used as permanent entity ids.
  *
  * @tparam T
  *     the type of the id to convert.
  */
sealed trait AsPermanentEntityId[T] {
  protected[datomisca] def conv(t: T): Long
}

/**
  * The three cases for converting permanent entity ids.
  */
object AsPermanentEntityId {

  /** Any type viewable as a Long can be a permanent entity id. */
  implicit def long[L](implicit toLong: L => Long) =
    new AsPermanentEntityId[L] {
      override protected[datomisca] def conv(l: L) = toLong(l)
    }

  /** A [[DLong]] can be a permament entity id. */
  implicit val dlong =
    new AsPermanentEntityId[DLong] {
      override protected[datomisca] def conv(l: DLong) = l.underlying
    }

  /** A [[FinalId]] can be a permament entity id. */
  implicit val finalid =
    new AsPermanentEntityId[FinalId] {
      override protected[datomisca] def conv(l: FinalId) = l.underlying
    }
}



final class DColl(coll: Iterable[DatomicData]) extends DatomicData {
  def toNative: AnyRef =
    datomic.Util.list(coll.map(_.toNative).toSeq : _*)

  override def toString = coll.mkString("[", ", ", "]")

  def toIterable = coll
  def toSet = coll.toSet
  def toSeq = coll.toSeq
}

object DColl {
  // def apply[DD <: DatomicData](set: Set[DD] = Set.empty) = new DColl(set map (_.asInstanceOf[DatomicData]))
  def apply[DD <: DatomicData](coll: Iterable[DD] = Iterable.empty) = new DColl(coll)
  def apply(dds: DatomicData*) = new DColl(dds)

  def unapply(dcoll: DColl): Option[Iterable[DatomicData]] = Some(dcoll.toIterable)
}


final case class DRuleAlias(name: String, args: Seq[Var], rules: Seq[Rule]) extends DatomicData {
  override def toNative = toString
  override def toString = "[ [%s %s] %s ]".format(
    name, 
    args.map(_.toString).mkString("", " ", ""),
    rules.map(_.toString).mkString("", " ", "")
  )
}

final case class DRuleAliases(aliases: Seq[DRuleAlias]) extends DatomicData {
  override def toNative = toString
  override def toString = "[ %s ]".format(
    aliases.map(_.toString).mkString("", " ", "")
  )
}


final class DDatom(
    val underlying: datomic.Datom,
    val database: DDatabase
) extends DatomicData {

  def id:     Long        = underlying.e.asInstanceOf[Long]
  def attr:   Keyword     = database.ident(attrId)
  def attrId: Int         = underlying.a.asInstanceOf[Integer]
  def value:  DatomicData = DatomicData.toDatomicData(underlying.v)
  def tx:     Long        = underlying.tx.asInstanceOf[Long]
  def added:  Boolean     = underlying.added

  override def toNative = underlying

  override def toString = "Datom(e[%s] a[%s] v[%s] tx[%s] added[%s])".format(id, attr, value, tx, added)
}

object DDatom {
   def unapply(da:DDatom):Option[(Long,Keyword,DatomicData,Long,Boolean)] = Some((da.id,da.attr,da.value,da.tx,da.added))
}
