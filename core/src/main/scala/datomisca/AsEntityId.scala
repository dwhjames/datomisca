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
  * The two cases for converting entity ids.
  */
object AsEntityId {

  /** Any type viewable as a Long can be an entity id. */
  implicit def long[L](implicit toLong: L => Long): AsEntityId[L] =
    new AsEntityId[L] {
      override protected[datomisca] def conv(l: L) = new FinalId(toLong(l))
    }

  /** Any subtype of [[DId]] can be an entity id. */
  implicit def dId[I <: DId]: AsEntityId[I] =
    new AsEntityId[I] {
      override protected[datomisca] def conv(i: I) = i
    }
}
