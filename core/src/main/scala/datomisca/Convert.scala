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


private[datomisca] object Convert {

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
    case kw: clojure.lang.Keyword => kw
    // :db.type/bytes
    case bytes: Array[Byte] => bytes
    // an entity map
    case e: datomic.Entity => new Entity(e)
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
