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


sealed trait DId extends Any {
  def toDatomicId: AnyRef
}

final class FinalId(val underlying: Long) extends AnyVal with DId {
  override def toDatomicId: AnyRef = underlying: java.lang.Long

  override def toString = underlying.toString
}

final class TempId(partition: Partition, id: Option[Long] = None, dbId: AnyRef) extends DId {
  override val toDatomicId: AnyRef = dbId

  override def toString = toDatomicId.toString
}

object DId {
  def tempid(partition: Partition, id: Option[Long] = None) = id match {
    case None => datomic.Peer.tempid(partition.keyword)
    case Some(id) => datomic.Peer.tempid(partition.keyword, id)
  }

  def apply(partition: Partition, id: Long) = new TempId(partition, Some(id), DId.tempid(partition, Some(id)))
  def apply(partition: Partition) = new TempId(partition, None, DId.tempid(partition))
  def apply[T](id: T)(implicit ev: AsEntityId[T]) = ev.conv(id)
}
