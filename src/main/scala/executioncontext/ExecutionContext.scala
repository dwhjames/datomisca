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

package datomisca.executioncontext

import java.util.concurrent.{ LinkedBlockingQueue, Callable, Executor, ExecutorService, Executors, ThreadFactory, TimeUnit, ThreadPoolExecutor }
import java.util.Collection
import scala.concurrent.forkjoin._
import scala.concurrent.{ BlockContext, ExecutionContext, Awaitable, CanAwait, ExecutionContextExecutor, ExecutionContextExecutorService }
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal


/**
 * Just shamefully copying scala ExecutionContext implementation based on ForkJoinPool
 * and simplifying it a bit for my purpose ;)
 */
class CustomExecutionContext(reporter: Throwable => Unit, min: Option[Int], num: Option[Int], max: Option[Int]) extends ExecutionContextExecutor {

  lazy val executor: Executor = executorService
  lazy val executorService: ExecutorService = createExecutorService

  // Implement BlockContext on FJP threads
  class DefaultThreadFactory(daemonic: Boolean) extends ThreadFactory with ForkJoinPool.ForkJoinWorkerThreadFactory {
    def wire[T <: Thread](thread: T): T = {
      thread.setDaemon(daemonic)
      //Potentially set things like uncaught exception handler, name etc
      thread
    }

    def newThread(runnable: Runnable): Thread = wire(new Thread(runnable))

    def newThread(fjp: ForkJoinPool): ForkJoinWorkerThread = wire(new ForkJoinWorkerThread(fjp) with BlockContext {
      override def blockOn[T](thunk: =>T)(implicit permission: CanAwait): T = {
        var result: T = null.asInstanceOf[T]
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker {
          @volatile var isdone = false
          override def block(): Boolean = {
            result = try thunk finally { isdone = true }
            true
          }
          override def isReleasable = isdone
        })
        result
      }
    })
  }

  def createExecutorService: ExecutorService = {
    println("Creating ExecutionContext with thread(min:%s num:%s max:%s)".format(min, num, max))
    def range(floor: Int, desired: Int, ceiling: Int): Int =
      if (ceiling < floor) range(ceiling, desired, floor) else scala.math.min(scala.math.max(desired, floor), ceiling)

    lazy val desiredParallelism = {
      range(
        min.getOrElse(0),
        num.getOrElse(Runtime.getRuntime.availableProcessors),
        max.getOrElse(Runtime.getRuntime.availableProcessors)
      )
    }

    val threadFactory = new DefaultThreadFactory(daemonic = true)

    try {
      new ForkJoinPool(
        desiredParallelism,
        threadFactory,
        null, //FIXME we should have an UncaughtExceptionHandler, see what Akka does
        true) // Async all the way baby
    } catch {
      case NonFatal(t) =>
        System.err.println("Failed to create ForkJoinPool for the default ExecutionContext, falling back to ThreadPoolExecutor")
        t.printStackTrace(System.err)
        val exec = new ThreadPoolExecutor(
          desiredParallelism,
          desiredParallelism,
          5L,
          TimeUnit.MINUTES,
          new LinkedBlockingQueue[Runnable],
          threadFactory
        )
        exec.allowCoreThreadTimeOut(true)
        exec
    }
  }

  def execute(runnable: Runnable): Unit = {
    executor match {
    case fj: ForkJoinPool =>
      Thread.currentThread match {
        case fjw: ForkJoinWorkerThread if fjw.getPool eq fj =>
          (runnable match {
            case fjt: ForkJoinTask[_] => fjt
            case _ => ForkJoinTask.adapt(runnable)
          }).fork
        case _ => fj.execute(runnable)
      }
    case generic =>
      generic execute runnable
  }}

  def reportFailure(t: Throwable) = reporter(t)
}