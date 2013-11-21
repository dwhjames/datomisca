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


/** Injective form of DatomicData to Scala converter :
  * - 1 DD => 1 Scala type
  * - used when precise type inference by compiler
  */
@implicitNotFound("There is no unique conversion from Datomic data type ${DD} to type ${A}")
private[datomisca] trait FromDatomicInj[DD <: AnyRef, A] {
  def from(dd: DD): A
}

private[datomisca] object FromDatomicInj extends FromDatomicInjImplicits {
  def apply[DD <: AnyRef, A](f: DD => A) = new FromDatomicInj[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}


/** Surjective for DatomicData to Scala converter :
  * - n DD => 1 Scala type
  */
@implicitNotFound("There is no conversion from Datomic data type ${DD} to type ${A}. Consider implementing an instance of the FromDatomic type class.")
trait FromDatomic[DD <: AnyRef, A] {
  def from(dd: DD): A
}

object FromDatomic extends FromDatomicImplicits {
  def apply[DD <: AnyRef, A](f: DD => A) = new FromDatomic[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}

/** Generic DatomicData to Scala type 
  * Multi-valued "function" (not real function actually) 
  * which inverse is surjective ToDatomic or ToDatomicCast
  * 1 DatomicData -> n Scala type
  */
@implicitNotFound("There is no cast available from Datomic data to type ${A}")
trait FromDatomicCast[A] {
  def from(dd: AnyRef): A
}

object FromDatomicCast extends FromDatomicCastImplicits {
  def apply[A](f: AnyRef => A) = new FromDatomicCast[A] {
    def from(dd: AnyRef): A = f(dd)
  }
}

/** Injective form of Scala to Specific DatomicData converters
  * 1 Scala type => 1 DD
  */
@implicitNotFound("There is no unique conversion from type ${A} to Datomic data type ${DD}")
trait ToDatomicInj[DD <: AnyRef, A] {
  def to(a: A): DD
}

object ToDatomicInj extends ToDatomicInjImplicits {
  def apply[DD <: AnyRef, A](f: A => DD) = new ToDatomicInj[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Surjective form of Scala to Specific DatomicData converters
  * n Scala type => 1 DD
  */
@implicitNotFound("There is no conversion from type ${A} to Datomic data type ${DD}. Consider implementing an instance of the ToDatomic type class.")
trait ToDatomic[DD <: AnyRef, A] {
  def to(a: A): DD
}

object ToDatomic extends ToDatomicImplicits{
  def apply[DD <: AnyRef, A](f: A => DD) = new ToDatomic[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Scala type to Generic DatomicData (surjective)
  * n Scala type -> DatomicData
  */
@implicitNotFound("There is no cast available from type ${A} to Datomic data")
trait ToDatomicCast[A] {
  def to(a: A): AnyRef
}

object ToDatomicCast extends ToDatomicCastImplicits {
  def apply[A](f: A => AnyRef) = new ToDatomicCast[A] {
    def to(a: A): AnyRef = f(a)
  }
}
