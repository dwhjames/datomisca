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

import scala.annotation.implicitNotFound


private[datomisca] object DatomicData {

  /** Converts any data to a Datomic Data (or not if not possible) */
  private[datomisca] def toScala(v: AnyRef): Any = v match {
    // :db.type/string
    case s: java.lang.String => s
    // :db.type/boolean
    case b: java.lang.Boolean => b: Boolean
    // :db.type/long
    case l: java.lang.Long => l: Long
    // attribute id
    case i: java.lang.Integer => i.toLong: Long
    // :db.type/float
    case f: java.lang.Float => f: Float
    // :db.type/double
    case d: java.lang.Double => d: Double
    // :db.type/bigint
    case bi: java.math.BigInteger => BigInt(bi)
    // :db.type/bigdec
    case bd: java.math.BigDecimal => BigDecimal(bd)
    // :db.type/instant
    case d: java.util.Date => d
    // :db.type/uuid
    case u: java.util.UUID => u
    // :db.type/uri
    case u: java.net.URI => u
    // :db.type/keyword
    case kw: clojure.lang.Keyword => 
      Keyword(kw.getName, Option(kw.getNamespace).map(Namespace(_)))
    // :db.type/bytes
    case bytes: Array[Byte] => bytes
    // an entity map
    case e: datomic.Entity => DEntity(e)
    // a collection
    case coll: java.util.Collection[_] =>
      new Iterable[Any] {
        override def iterator = new Iterator[Any] {
          private val jIter = coll.iterator.asInstanceOf[java.util.Iterator[AnyRef]]
          override def hasNext = jIter.hasNext
          override def next() = toScala(jIter.next())
        }
      }
    // otherwise
    case v => throw new UnsupportedDatomicTypeException(v.getClass)
  }

}

object DRef

@implicitNotFound("Cannot use type ${T} as the type of a Datomic reference")
sealed trait ToDRef[T] {
  def toDRef(t: T): AnyRef
}

object ToDRef {

  implicit val long2DRef: ToDRef[Long] =
    new ToDRef[Long] {
      def toDRef(l:  Long) = l: java.lang.Long
    }

  implicit def did2DRef[I <: DId]: ToDRef[I] =
    new ToDRef[I] {
      def toDRef(i:  I) = i.toNative
    }

  implicit def keyword2DRef[K](implicit toKeyword: K => Keyword): ToDRef[K] =
    new ToDRef[K] {
      def toDRef(k: K) = toKeyword(k).toNative
    }

  implicit def tempIdentified2DRef[I <: TempIdentified]: ToDRef[I] =
    new ToDRef[I] {
      def toDRef(i: I) = i.id.toNative
    }

  implicit def finalIdentified2DRef[I <: FinalIdentified]: ToDRef[I] =
    new ToDRef[I] {
      def toDRef(i: I) = i.id: java.lang.Long
    }

  implicit def keywordIdentified2DRef[I <: KeywordIdentified]: ToDRef[I] =
    new ToDRef[I] {
      def toDRef(i: I) = i.ident.toNative
    }
}

sealed trait DId extends Nativeable

final class FinalId(val underlying: Long) extends DId {
  override def toNative: AnyRef = underlying: java.lang.Long

  override def toString = toNative.toString
}

final case class TempId(partition: Partition, id: Option[Long] = None, dbId: AnyRef) extends DId {
  override val toNative: AnyRef = dbId

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
@implicitNotFound("Cannot convert value of type ${T} to a Datomic entity id")
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
@implicitNotFound("Cannot convert value of type ${T} to a permanent Datomic entity id")
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

  /** A [[FinalId]] can be a permament entity id. */
  implicit val finalid =
    new AsPermanentEntityId[FinalId] {
      override protected[datomisca] def conv(l: FinalId) = l.underlying
    }
}


final class DRules(val edn: clojure.lang.IPersistentVector) extends AnyVal {
  override def toString = edn.toString
}


final class DDatom(
    val underlying: datomic.Datom
) extends AnyVal {

  def id:     Long        = underlying.e.asInstanceOf[Long]
  def attrId: Int         = underlying.a.asInstanceOf[Integer]
  def value:  Any         = DatomicData.toScala(underlying.v)
  def tx:     Long        = underlying.tx.asInstanceOf[Long]
  def added:  Boolean     = underlying.added

  override def toString = "Datom(e[%s] a[%s] v[%s] tx[%s] added[%s])".format(id, attrId, value, tx, added)
}

object DDatom {
   def unapply(da:DDatom):Option[(Long,Int,Any,Long,Boolean)] = Some((da.id,da.attrId,da.value,da.tx,da.added))
}
