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

import scala.concurrent.blocking

import clojure.lang.Keyword


class DEntity(val entity: datomic.Entity) extends AnyVal {
  def toNative = entity

  def id: Long = as[Long](Namespace.DB / "id")

  def touch() = {
    blocking { entity.touch() }
    this
  }

  def apply(keyword: Keyword): Any =
    get(keyword) getOrElse { throw new EntityKeyNotFoundException(keyword.toString) }

  def get(keyword: Keyword): Option[Any] =
    Option {
      entity.get(keyword)
    } map (DatomicData.toScala(_))

  def apply(keyword: String): Any =
    get(keyword) getOrElse { throw new EntityKeyNotFoundException(keyword) }

  def get(keyword: String): Option[Any] =
    Option {
      entity.get(keyword)
    } map (DatomicData.toScala(_))

  def as[T](keyword: Keyword)(implicit fdat: FromDatomicCast[T]): T =
    getAs[T](keyword) getOrElse { throw new EntityKeyNotFoundException(keyword.toString) }

  def getAs[T](keyword: Keyword)(implicit fdat: FromDatomicCast[T]): Option[T] =
    Option {
      entity.get(keyword)
    } map (fdat.from)

  def keySet: Set[String] = {
    val builder = Set.newBuilder[String]
    val iter = blocking { entity.keySet } .iterator
    while (iter.hasNext) {
      builder += iter.next()
    }
    builder.result
  }

  def toMap: Map[String, Any] = {
    val builder = Map.newBuilder[String, Any]
    val iter = blocking { entity.keySet } .iterator
    while (iter.hasNext) {
      val key = iter.next()
      builder += (key -> DatomicData.toScala(entity.get(key)))
    }
    builder.result
  }

}

object DEntity {
  def apply(ent: datomic.Entity) = new DEntity(ent)
}
