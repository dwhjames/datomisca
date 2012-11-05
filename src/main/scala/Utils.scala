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