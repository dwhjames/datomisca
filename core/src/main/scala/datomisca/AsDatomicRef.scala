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


@implicitNotFound("Cannot use type ${T} as the type of a Datomic reference")
sealed trait AsDatomicRef[T] {
  def toDatomicRef(t: T): AnyRef
}

object AsDatomicRef {

  implicit val long: AsDatomicRef[Long] =
    new AsDatomicRef[Long] {
      def toDatomicRef(l:  Long) = l: java.lang.Long
    }

  implicit def dId[I <: DId]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i:  I) = i.toDatomicId
    }

  implicit def keyword[K](implicit toKeyword: K => Keyword): AsDatomicRef[K] =
    new AsDatomicRef[K] {
      def toDatomicRef(k: K) = toKeyword(k)
    }

  implicit def tempIdentified[I <: TempIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.id.toDatomicId
    }

  implicit def finalIdentified[I <: FinalIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.id.toDatomic
    }

  implicit def keywordIdentified[I <: KeywordIdentified]: AsDatomicRef[I] =
    new AsDatomicRef[I] {
      def toDatomicRef(i: I) = i.ident
    }
}
