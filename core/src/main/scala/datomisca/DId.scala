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

import scala.language.existentials

import datomic.Util

sealed trait DId extends Any {
  def toDatomicId: AnyRef
}

final class FinalId(val underlying: Long) extends AnyVal with DId {
  override def toDatomicId: AnyRef = underlying: java.lang.Long

  override def toString = underlying.toString
}

final class TempId(val underlying: datomic.db.DbId) extends AnyVal with DId {
  override def toDatomicId: AnyRef = underlying

  def part: Partition =
    new Partition(underlying.get(TempId.part).asInstanceOf[Keyword])

  def idx: Long =
    underlying.get(TempId.idx).asInstanceOf[java.lang.Long]

  override def toString: String = underlying.toString
}

final class LookupRef(val underlying: java.util.List[_]) extends AnyVal with DId {
  def toDatomicId = underlying
  override def toString = underlying.toString

  /** Returns the referenced entity, or none if the
    * lookup ref does not resolve.
    */
  def entity(implicit db: Database): Option[Entity] = {
    val e = db.underlying.entity(this.underlying)
    if (e ne null)
      Some(new Entity(e))
    else
      None
  }

  /** Returns the referenced entity id, or none if the
    * lookup ref does not resolve.
    */
  def entid(implicit db: Database): Option[Long] = {
    val l = db.underlying.entid(this.underlying).asInstanceOf[java.lang.Long]
    if (l ne null)
      Some(l.asInstanceOf[Long])
    else
      None
  }
}

object LookupRef {
  def apply[DD <: AnyRef, T](attr: Attribute[DD, Cardinality.one.type], value: T)
                            (implicit toDatomic: ToDatomic[DD, T]) =
    new LookupRef(Util.list(attr.ident, toDatomic.to(value)))
}

private object TempId {
  private val part: Keyword = clojure.lang.Keyword.intern(null, "part")
  private val idx:  Keyword = clojure.lang.Keyword.intern(null, "idx")
}

object DId {

  def apply(partition: Partition, id: Long) =
    new TempId(datomic.Peer.tempid(partition.keyword, id).asInstanceOf[datomic.db.DbId])

  def apply(partition: Partition) =
    new TempId(datomic.Peer.tempid(partition.keyword).asInstanceOf[datomic.db.DbId])

  def apply[T](id: T)(implicit ev: AsEntityId[T]) = ev.conv(id)
}
