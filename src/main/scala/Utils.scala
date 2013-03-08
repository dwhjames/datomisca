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

import scala.language.higherKinds
import scala.language.implicitConversions

import datomic.ListenableFuture

import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.{TimeUnit, Executor}

import scala.util.{Try, Success, Failure}
import scala.collection.generic.CanBuildFrom
import scala.collection.TraversableLike

object Utils {
  def bridgeDatomicFuture[T](listenF: ListenableFuture[T])(implicit ex: ExecutionContext): Future[T] = {
    val p = Promise[T]

    listenF.addListener(
      new java.lang.Runnable {
        override def run: Unit =
          try {
            p.success(listenF.get())
          } catch {
            case ex: java.util.concurrent.ExecutionException =>
              p.failure(ex.getCause)
            case ex: Throwable =>
              p.failure(ex)
          }
      },
      new java.util.concurrent.Executor {
        def execute(arg0: Runnable): Unit = ex.execute(arg0)
      }
    )

    p.future
  }

  def sequence[A, M[_]](l: M[Try[A]])
    (implicit toTraversableLike: M[Try[A]] => TraversableLike[Try[A], M[Try[A]]], 
    cbf: CanBuildFrom[M[_], A, M[A]]): Try[M[A]] = {
    l.foldLeft(Success(cbf()): Try[scala.collection.mutable.Builder[A, M[A]]]){ (acc, e) => e match {
      case Failure(e) => Failure(e)
      case Success(s) => acc.map{ acc => acc += s }
    }}.map(_.result)
  }

  /** Converts a java.util.Map[_,_] returns by connection.transact into a TxReport 
    * It requires an implicit DDatabase because it must resolve the keyword from Datom (from Integer in the map)
    */
  def toTxReport(javaMap: java.util.Map[_, _])(implicit database: DDatabase): TxReport = {
    import scala.collection.JavaConverters._
    import datomic.Connection._
    import datomic.db.Db

    new TxReport {
      override val dbBefore = DDatabase(
          javaMap.get(DB_BEFORE).asInstanceOf[Db]
        )
      override val dbAfter  = DDatabase(
          javaMap.get(DB_AFTER).asInstanceOf[Db]
        )
      override val txData =
        javaMap.get(TX_DATA)
               .asInstanceOf[java.util.List[datomic.Datom]]
               .asScala
               .map(DDatom(_)(database))
               .toSeq
      override protected val tempids = javaMap.get(TEMPIDS).asInstanceOf[AnyRef]
    }
  }

  def queue2Stream[A](queue: java.util.concurrent.BlockingQueue[A]): Stream[Option[A]] = {
    def toStream: Stream[Option[A]] = {
      Option(queue.poll()) match {
        case None => Stream.cons(None, toStream)
        case Some(a) => Stream.cons(Some(a), toStream)
      }
    }

    toStream
  }

}

trait TxReportQueue {
  implicit def database: DDatabase

  def queue: java.util.concurrent.BlockingQueue[java.util.Map[_, _]]

  lazy val stream: Stream[Option[TxReport]] = Utils.queue2Stream[java.util.Map[_, _]](queue).map{ 
    case None => None
    case Some(javaMap) => Some(Utils.toTxReport(javaMap)(database))
  }
}

/**
 * Combination operator
 */
case class ~[A,B](_1:A, _2:B)

trait Variant[M[_]]

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


class Builder[M[_]](combi: Combinator[M]) {
  class Builder2[A1, A2](m1: M[A1], m2: M[A2]) {
    def ~[A3](m3: M[A3]) = Builder3(combi(m1, m2), m3)
    def and[A3](m3: M[A3]) = this.~(m3)

    def apply[B](f: (A1, A2) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2, B]( combi(m1, m2), { case a1 ~ a2 => f(a1, a2) } )

    def apply[B](f: B => (A1, A2))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2) => new ~(a1, a2) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2) => (a1, a2) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2) => (a._1, a._2) }(f)
    }
  }

  case class Builder3[A1, A2, A3](m1: M[A1 ~ A2], m2: M[A3]) {
    def ~[A4](m3: M[A4]) = Builder4(combi(m1, m2), m3)
    def and[A4](m3: M[A4]) = this.~(m3)

    def apply[B](f: (A1, A2, A3) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 => f(a1, a2, a3) } )

    def apply[B](f: B => (A1, A2, A3))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3) => new ~(new ~(a1, a2), a3) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3) => (a1, a2, a3) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3) => (a._1, a._2, a._3) }(f)
    }
  }

  case class Builder4[A1, A2, A3, A4](m1: M[A1 ~ A2 ~ A3], m2: M[A4]) {
    def ~[A5](m3: M[A5]) = Builder5(combi(m1, m2), m3)
    def and[A5](m3: M[A5]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 => f(a1, a2, a3, a4) } )

    def apply[B](f: B => (A1, A2, A3, A4))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4) => new ~(new ~(new ~(a1, a2), a3), a4) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4) => (a1, a2, a3, a4) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4) => (a._1, a._2, a._3, a._4) }(f)
    }
  }

  case class Builder5[A1, A2, A3, A4, A5](m1: M[A1 ~ A2 ~ A3 ~ A4], m2: M[A5]) {
    def ~[A6](m3: M[A6]) = Builder6(combi(m1, m2), m3)
    def and[A6](m3: M[A6]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 => f(a1, a2, a3, a4, a5) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5) => new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => (a1, a2, a3, a4, a5) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5) => (a._1, a._2, a._3, a._4, a._5) }(f)
    }
  }

  case class Builder6[A1, A2, A3, A4, A5, A6](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5], m2: M[A6]) {
    def ~[A7](m3: M[A7]) = Builder7(combi(m1, m2), m3)
    def and[A7](m3: M[A7]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 => f(a1, a2, a3, a4, a5, a6) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6) => new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6) => (a1, a2, a3, a4, a5, a6) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6) => (a._1, a._2, a._3, a._4, a._5, a._6) }(f)
    }
  }

  case class Builder7[A1, A2, A3, A4, A5, A6, A7](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6], m2: M[A7]) {
    def ~[A8](m3: M[A8]) = Builder8(combi(m1, m2), m3)
    def and[A8](m3: M[A8]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 => f(a1, a2, a3, a4, a5, a6, a7) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7) => new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7) => (a1, a2, a3, a4, a5, a6, a7) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7) }(f)
    }
  }

  case class Builder8[A1, A2, A3, A4, A5, A6, A7, A8](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7], m2: M[A8]) {
    def ~[A9](m3: M[A9]) = Builder9(combi(m1, m2), m3)
    def and[A9](m3: M[A9]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 => f(a1, a2, a3, a4, a5, a6, a7, a8) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8) => (a1, a2, a3, a4, a5, a6, a7, a8) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8) }(f)
    }
  }

  case class Builder9[A1, A2, A3, A4, A5, A6, A7, A8, A9](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8], m2: M[A9]) {
    def ~[A10](m3: M[A10]) = Builder10(combi(m1, m2), m3)
    def and[A10](m3: M[A10]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8~A9, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8~A9, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8, a9) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9) => (a1, a2, a3, a4, a5, a6, a7, a8, a9) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8, A9) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9) }(f)
    }
  }

  case class Builder10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9], m2: M[A10]) {
    def ~[A11](m3: M[A11]) = Builder11(combi(m1, m2), m3)
    def and[A11](m3: M[A11]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10) }(f)
    }
  }

  case class Builder11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10], m2: M[A11]) {
    def ~[A12](m3: M[A12]) = Builder12(combi(m1, m2), m3)
    def and[A12](m3: M[A12]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11) }(f)
    }
  }

  case class Builder12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11], m2: M[A12]) {
    def ~[A13](m3: M[A13]) = Builder13(combi(m1, m2), m3)
    def and[A13](m3: M[A13]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11~A12, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11~A12, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12) }(f)
    }
  }  

  case class Builder13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12], m2: M[A13]) {
    def ~[A14](m3: M[A14]) = Builder14(combi(m1, m2), m3)
    def and[A14](m3: M[A14]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11~A12~A13, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 ~ a6 ~ a7 ~ a8 ~ a9 ~ a10 ~ a11 ~ a12 ~ a13 => f(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5~A6~A7~A8~A9~A10~A11~A12~A13, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) => new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5), a6), a7), a8), a9), a10), a11), a12), a13) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13) => (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => (a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13) }(f)
    }
  }  

  case class Builder14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5 ~ A6 ~ A7 ~ A8 ~ A9 ~ A10 ~ A11 ~ A12 ~ A13], m2: M[A14])
}

trait Monad[M[_]] {
  def unit[A](a: A): M[A]
  def bind[A, B](ma: M[A], f: A => M[B]): M[B]
}

object CombinatorImplicits extends CombinatorImplicits

trait CombinatorImplicits {
  implicit def RDCombinatorOpsWrapper[M[_] <: EntityMapper[_], A](ma: M[A])(implicit combi: Combinator[M]) = new CombinatorOps(ma)(combi)

  implicit def RDCombinatorWrapper[M[_] <: EntityReader[_]](implicit monad: Monad[M]) = new Combinator[M] {
    def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B] = monad.bind(ma, (a: A) => monad.bind(mb, (b: B) => monad.unit(new ~(a, b)) ))
  }

  implicit def RDtoFunctorOps[M[_] <: EntityReader[_], A](ma: M[A])(implicit fu: Functor[M]): FunctorOps[M, A] = new FunctorOps(ma)
  implicit def RDtoContraFunctorOps[M[_] <: PartialAddEntityWriter[_], A](ma: M[A])(implicit fu: ContraFunctor[M]): ContraFunctorOps[M, A] = new ContraFunctorOps(ma)

  def unlift[A, B](f: A => Option[B]): A => B = Function.unlift(f)

}