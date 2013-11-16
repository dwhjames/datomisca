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
import scala.util.control.NonFatal

import datomic.ListenableFuture


class Connection(val connection: datomic.Connection) extends AnyVal {

  def database: Database = new Database(connection.db())

  /**
    * Used to coordinate with other peers.
    *
    * Returns a future that will acquire a
    * database value guaranteed to include all transactions that were
    * complete at the time sync was called.  Communicates with the
    * transactor.
    *
    * db is the preferred way to get a database value, as it does not
    * need to wait nor block. Only use sync when coordination is
    * required, and prefer the one-argument version when you have a
    * basis t.
    *
    * (Copied from the Datomic docs.)
    *
    * @param exec
    *     an implicit execution context.
    * @return Returns a future that will acquire a
    * database value guaranteed to include all transactions that were
    * complete at the time sync was called.
    */
  def sync()(implicit exec: ExecutionContext): Future[Database] =
    Connection.bridgeDatomicFuture(connection.sync()) map (new Database(_))

  /**
    * Used to coordinate with other peers.
    *
    * Returns a future that will acquire a
    * database value with basisT >= t. Does not communicate with the
    * transactor.
    *
    * db is the preferred way to get a database value, as it does not
    * need to wait nor block. Only use sync when coordination is
    * required, and prefer the one-argument version when you have a
    * basis t.
    *
    * (Copied from the Datomic docs.)
    *
    * @param t
    *     a transaction number.
    * @param exec
    *     an implicit execution context.
    * @return Returns a future that will acquire a
    * database value guaranteed to include all transactions that were
    * complete at the time sync was called.
    */
  def sync(t: Long)(implicit exec: ExecutionContext): Future[Database] =
    Connection.bridgeDatomicFuture(connection.sync(t)) map (new Database(_))


  def transact(ops: Seq[TxData])(implicit ex: ExecutionContext): Future[TxReport] = {
    val future = try {
        Connection.bridgeDatomicFuture(connection.transactAsync(datomic.Util.list(ops.map(_.toTxData):_*)))
      } catch {
        case NonFatal(ex) => Future.failed(ex)
      }

    future map { javaMap: java.util.Map[_, _] =>
      new TxReport(javaMap)
    }
  }

  def transact(op: TxData)(implicit ex: ExecutionContext): Future[TxReport] = transact(Seq(op))
  def transact(op: TxData, ops: TxData *)(implicit ex: ExecutionContext): Future[TxReport] = transact(Seq(op) ++ ops)

  def txReportQueue: TxReportQueue = new TxReportQueue(connection.txReportQueue)

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

  /** Retrieves a value of the log for reading.
    *
    * Note: the mem db has no log, and thus for it log will return null.
    *
    * (Copied from Datomic docs.)
    *
    * @return the current value of the log.
    */
  def log(): Log = new Log(connection.log())
}

object Connection {

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
