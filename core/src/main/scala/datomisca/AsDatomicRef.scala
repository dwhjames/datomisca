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


/** A type class for types that can be used as reference values.
  *
  * @tparam T the type of values to use as a reference value.
  */
@implicitNotFound("Cannot use type ${T} as the type of a Datomic reference")
sealed trait AsDatomicRef[T] {

  /** Convert a value of type T into an Object supported by Datomic.
    *
    * Datomic supports `java.lang.Long` and [[Keyword]]
    * in the value position of a reference attribute.
    *
    * @param t a value to be treated as a reference value.
    * @return an Object supported by Datomic.
    */
  def toDatomicRef(t: T): AnyRef
}


/** The instances of the [[AsDatomicRef]] type class. */
object AsDatomicRef {

  /** A Scala Long is a valid reference type. */
  implicit val long: AsDatomicRef[Long] =
    new AsDatomicRef[Long] {
      def toDatomicRef(l:  Long) = l: java.lang.Long
    }

  /** Any subtype of [[DId]] is a valid reference type. */
  implicit def dId[I <: DId]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i:  I) = i.toDatomicId
    }

  /** Any type that can be viewed as a [[Keyword]] is a valid reference type. */
  implicit def keyword[K](implicit toKeyword: K => Keyword): AsDatomicRef[K] =
    new AsDatomicRef[K] {
      def toDatomicRef(k: K) = toKeyword(k)
    }

  /** Any subtype of [[TempIdentified]] is a valid reference type. */
  implicit def tempIdentified[I <: TempIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.id.toDatomicId
    }

  /** Any subtype of [[FinalIdentified]] is a valid reference type. */
  implicit def finalIdentified[I <: FinalIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.id
    }

  /** Any subtype of [[KeywordIdentified]] is a valid reference type. */
  implicit def keywordIdentified[I <: KeywordIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.ident
    }
}
