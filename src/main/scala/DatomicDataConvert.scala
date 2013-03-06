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

/** Injective form of DatomicData to Scala converter :
  * - 1 DD => 1 Scala type
  * - used when precise type inference by compiler
  */
private[datomisca] trait FromDatomicInj[DD <: DatomicData, A] {
  def from(dd: DD): A
}

private[datomisca] object FromDatomicInj extends FromDatomicInjImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new FromDatomicInj[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}


/** Surjective for DatomicData to Scala converter :
  * - n DD => 1 Scala type
  */
trait FromDatomic[DD <: DatomicData, A] {
  def from(dd: DD): A
}

object FromDatomic extends FromDatomicImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new FromDatomic[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}

/** Generic DatomicData to Scala type 
  * Multi-valued "function" (not real function actually) 
  * which inverse is surjective ToDatomic or ToDatomicCast
  * 1 DatomicData -> n Scala type
  */
trait FromDatomicCast[A] {
  def from(dd: DatomicData): A
}

object FromDatomicCast extends FromDatomicCastImplicits {
  def apply[A](f: DatomicData => A) = new FromDatomicCast[A] {
    def from(dd: DatomicData): A = f(dd)
  }
}

/** Injective form of Scala to Specific DatomicData converters
  * 1 Scala type => 1 DD
  */
trait ToDatomicInj[DD <: DatomicData, A] {
  def to(a: A): DD
}

object ToDatomicInj extends ToDatomicInjImplicits {
  def apply[DD <: DatomicData, A](f: A => DD) = new ToDatomicInj[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Surjective form of Scala to Specific DatomicData converters
  * n Scala type => 1 DD
  */
trait ToDatomic[DD <: DatomicData, A] {
  def to(a: A): DD
}

object ToDatomic extends ToDatomicImplicits{
  def apply[DD <: DatomicData, A](f: A => DD) = new ToDatomic[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Scala type to Generic DatomicData (surjective)
  * n Scala type -> DatomicData
  */
trait ToDatomicCast[A] {
  def to(a: A): DatomicData
}

object ToDatomicCast extends ToDatomicCastImplicits {
  def apply[A](f: A => DatomicData) = new ToDatomicCast[A] {
    def to(a: A): DatomicData = f(a)
  }
}


trait DatomicBij[DD <: DatomicData, A] extends FromDatomicInj[DD, A] with ToDatomicInj[DD, A]

object DatomicBij {
  def apply[DD <: DatomicData, A](fdat: FromDatomicInj[DD, A], tdat: ToDatomicInj[DD, A]) = 
    new DatomicBij[DD, A]{
      def from(dd: DD): A = fdat.from(dd)
      def to(a: A): DD    = tdat.to(a)
    }

  implicit def datomicBij[DD <: DatomicData, A](implicit fdat: FromDatomicInj[DD, A], tdat: ToDatomicInj[DD, A]) = 
    DatomicBij.apply(fdat, tdat)
}

