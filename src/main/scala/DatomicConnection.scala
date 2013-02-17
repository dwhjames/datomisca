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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


trait TxReport extends TxReportHidden {
  val dbBefore: DDatabase
  val dbAfter:  DDatabase
  val txData:   Seq[DDatom]
  protected val tempids: AnyRef

  override def resolve(id: DId)(implicit db: DDatabase): Long =
    resolveOpt(id) getOrElse { throw new TempidNotResolved(id) }
  
  def resolve(identified: Identified)(implicit db: DDatabase): Long =
    resolve(identified.id)

  def resolve(ids: Seq[DId])(implicit db: DDatabase): Seq[Long] =
    ids map { resolve(_) }

  def resolveOpt(id: DId)(implicit db: DDatabase): Option[Long] =
    Option {
      datomic.Peer.resolveTempid(db.underlying, tempids, id.toNative)
    } map { id =>
      id.asInstanceOf[Long]
    }
  
  def resolveOpt(ids: Seq[DId])(implicit db: DDatabase): Seq[Option[Long]] =
    ids map { resolveOpt(_) }
}

trait Connection {
  self =>
  def connection: datomic.Connection

  def database: DDatabase = DDatabase(connection.db())

  def transact(ops: Seq[Operation])(implicit ex: ExecutionContext): Future[TxReport] = {
    import scala.collection.JavaConverters._

    val datomicOps = ops.map( _.toNative ).asJava

    val future = Utils.bridgeDatomicFuture(connection.transactAsync(datomicOps))
    
    future.flatMap{ javaMap: java.util.Map[_, _] =>
      Future(Utils.toTxReport(javaMap)(database))
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

}

object Connection {
  def apply(conn: datomic.Connection) = new Connection {
    def connection = conn
  }
}