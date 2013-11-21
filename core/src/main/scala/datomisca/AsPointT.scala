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

import java.util.{Date => JDate}


/**
  * A conversion type class for point in time values.
  *
  * A type class for converting from various point in time values.
  * Basis T values as Long, transaction entity ids as Long, transaction
  * time stamps as java.util.Date, and transaction time stamps as
  * [[DInstant]].
  *
  * @tparam T
  *     the type of the point in time.
  */
@implicitNotFound("Cannot use a value of type ${T} as a point in time")
sealed trait AsPointT[T] {

  /**
    * Convert from a point in time.
    *
    * @param t
    *     a point in time.
    * @return an upcast of the point in time.
    */
  protected[datomisca] def conv(t: T): AnyRef
}

/**
  * The two cases for converting points in time.
  */
object AsPointT {

  /** Basis T and transaction entity id values as Long are points in time. */
  implicit val long =
    new AsPointT[Long] {
      override protected[datomisca] def conv(l: Long) = l: java.lang.Long
    }

  /** Transaction time stamps as java.util.Date are points in time. */
  implicit val jDate =
    new AsPointT[JDate] {
      override protected[datomisca] def conv(date: JDate) = date
    }

}
