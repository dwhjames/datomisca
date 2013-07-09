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

import scala.concurrent._

import datomic.ListenableFuture


trait Connection {
  self =>
  def connection: datomic.Connection

  def database: DDatabase = DDatabase(connection.db())

  def transact(ops: Seq[Operation])(implicit ex: ExecutionContext): Future[TxReport] = {
    import scala.collection.JavaConverters._

    val datomicOps = ops.map( _.toNative ).asJava

    val future = Connection.bridgeDatomicFuture(connection.transactAsync(datomicOps))

    future.flatMap{ javaMap: java.util.Map[_, _] =>
      Future(TxReport.toTxReport(javaMap)(database))
    }
  }

  def transact(op: Operation)(implicit ex: ExecutionContext): Future[TxReport] = transact(Seq(op))
  def transact(op: Operation, ops: Operation *)(implicit ex: ExecutionContext): Future[TxReport] = transact(Seq(op) ++ ops)

  def txReportQueue: TxReportQueue = new TxReportQueue {
    override implicit val database = self.database

    override val queue = connection.txReportQueue
  }

  def removeTxReportQueue: Unit = connection.removeTxReportQueue

  def requestIndex: Boolean = connection.requestIndex

  def gcStorage(olderThan: java.util.Date): Unit = connection.gcStorage(olderThan)

  /** Request the release of resources associated with this connection.
    * Copied from Datomic Javadoc: Method returns immediately, resources will be
    * released asynchronously. This method should only be called when the entire
    * process is no longer interested in the connection. Note that Datomic
    * connections do not adhere to an acquire/use/release pattern.
    * They are thread-safe, cached, and long lived. Many processes
    * (e.g. application servers) will never call release.
    */
  def release(): Unit = connection.release()
}

object Connection {
  def apply(conn: datomic.Connection) = new Connection {
    def connection = conn
  }


  private[datomisca] def bridgeDatomicFuture[T](listenF: ListenableFuture[T])(implicit ex: ExecutionContext): Future[T] = {
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
}
