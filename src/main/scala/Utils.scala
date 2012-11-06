package reactivedatomic

import scala.concurrent.{Future, Promise}
import datomic.ListenableFuture

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.util.{Try, Success, Failure}

object Utils {
  def bridgeDatomicFuture[T](listenF: ListenableFuture[T])(implicit ex: ExecutionContext with Executor): Future[T] = {
    val p = Promise[T]

    listenF.addListener(
      new java.lang.Runnable {
        override def run: Unit = {
          p.complete(
            try {
              Success(listenF.get(0, MILLISECONDS))
            }catch {
              case e => Failure(e)
            }
          )
        }
      },
      ex
    )

    p.future
  }

}

/**
 * Combination operator
 */
case class ~[A,B](_1:A, _2:B)

/**
 * Combinator base trait
 */
trait Combinator[M[_]] {
  def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B]
}

class CombinatorOps[A, M[_]](ma: M[A])(implicit combi: Combinator[M]) {
  def ~[B](mb: M[B]): M[A ~ B] = combi(ma, mb)
  def and[B](mb: M[B]): M[A ~ B] = ~(ma, mb)
}

trait CombinatorImplicits {
  implicit def CombinatorOpsWrapper[A, M[_]](ma: M[A])(implicit combi: Combinator[M]) = new CombinatorOps(ma)(combi)
}