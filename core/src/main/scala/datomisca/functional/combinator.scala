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
package functional

import scala.language.higherKinds
import scala.language.implicitConversions


/**
 * Combination operator
 */
final case class ~[A,B](_1:A, _2:B)

sealed trait Variant[M[_]]

trait Functor[M[_]] extends Variant[M] {
  def fmap[A, B](ma: M[A], f: A => B): M[B]
}

trait ContraFunctor[M[_]] extends Variant[M] {
  def contramap[A, B](ma: M[A], f: B => A): M[B]
}

class FunctorOps[M[_],A](ma: M[A])(implicit fu: Functor[M]){
  def fmap[B](f: A => B): M[B] = fu.fmap(ma, f)
}

class ContraFunctorOps[M[_],A](ma:M[A])(implicit fu:ContraFunctor[M]){
  def contramap[B](f: B => A): M[B] = fu.contramap(ma, f)
}


/**
 * Combinator base trait
 */
trait Combinator[M[_]] {
  def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B]
}

class CombinatorOps[M[_], A](ma: M[A])(implicit combi: Combinator[M]) {
  def ~[B](mb: M[B]) = {
    val builder = new Builder(combi)
    new builder.Builder2(ma, mb)
  }
  def and[B](mb: M[B]) = this.~(mb)
}

trait Monad[M[_]] {
  def unit[A](a: A): M[A]
  def bind[A, B](ma: M[A], f: A => M[B]): M[B]
}


trait CombinatorImplicits {
  implicit def RDCombinatorOpsWrapper[M[_] <: EntityMapper[_], A](ma: M[A])(implicit combi: Combinator[M]) = new CombinatorOps(ma)(combi)

  implicit def RDCombinatorWrapper[M[_] <: EntityReader[_]](implicit monad: Monad[M]) = new Combinator[M] {
    def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B] = monad.bind(ma, (a: A) => monad.bind(mb, (b: B) => monad.unit(new ~(a, b)) ))
  }

  implicit def RDtoFunctorOps[M[_] <: EntityReader[_], A](ma: M[A])(implicit fu: Functor[M]): FunctorOps[M, A] = new FunctorOps(ma)
  implicit def RDtoContraFunctorOps[M[_] <: PartialAddEntityWriter[_], A](ma: M[A])(implicit fu: ContraFunctor[M]): ContraFunctorOps[M, A] = new ContraFunctorOps(ma)

  def unlift[A, B](f: A => Option[B]): A => B = Function.unlift(f)

}
