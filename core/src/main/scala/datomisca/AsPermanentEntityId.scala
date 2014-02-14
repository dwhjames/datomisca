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
  protected[datomisca] def conv(t: T): AnyRef
}

/**
  * The two cases for converting permanent entity ids.
  */
object AsPermanentEntityId {

  /** Any type viewable as a Long can be a permanent entity id. */
  implicit def long[L](implicit toLong: L => Long) =
    new AsPermanentEntityId[L] {
      override protected[datomisca] def conv(l: L) = toLong(l): java.lang.Long
    }

  /** A [[FinalId]] can be a permament entity id. */
  implicit val finalid =
    new AsPermanentEntityId[FinalId] {
      override protected[datomisca] def conv(l: FinalId) = l.underlying: java.lang.Long
    }

  /** A [[LookupRef]] can be a permament entity id. */
  implicit def lookupRefId =
    new AsPermanentEntityId[LookupRef] {
      override protected[datomisca] def conv(l: LookupRef) = l.toDatomicId
    }
}

