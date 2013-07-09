package datomisca.executioncontext

import java.util.concurrent.ExecutorService

/**
 * DefaultImplicits to import when using play2-async-nio outside Play2 to have implicit ExecutionContext and ExecutorService
 *
 * {{{import datomisca.executioncontext.ExecutionContextHelper._}}}
 */ 
object ExecutionContextHelper extends Context

/**
 * A Context containing the implicit ExecutionContext and ExecutorService
 * It creates an executioncontext based on a pool of threads defined by 3 parameters:
 *  - min : the minimum number of threads in the pool
 *  - num : the expected number of threads in the pool or a multiplier of the nb of cores (2x, 4x)
 *  - max : the maximum number of threads in the pool
 */ 
trait Context {
  /**
   * the minimum number of threads in the pool
   */ 
  def min: Option[Int] = None

  /**
   * the expected number of threads in the pool or a multiplier of the nb of cores (2x, 4x)
   */ 
  def num: Option[Int] = None

  /**
   * the maximum number of threads in the pool
   */ 
  def max: Option[Int] = None

  def defaultReporter: Throwable => Unit = (t: Throwable) => t.printStackTrace()

  /**
   * implicit ExecutionContext
   */
  implicit lazy val defaultExecutionContext: CustomExecutionContext = new CustomExecutionContext(defaultReporter, min, num, max)

  /**
   * implicit ExecutorService
   */
  implicit lazy val defaultExecutorService: ExecutorService = defaultExecutionContext.executorService

}
 

