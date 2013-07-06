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

import scala.concurrent.{Future, ExecutionContext}


/** Provides all Datomic Database Transactor asynchronous function.
  *
  * Please note that those functions require :
  *   - an implicit [[Connection]] for transaction,
  *   - Scala scala.concurrent.ExecutionContext for Future management
  */
trait TransactOps {

  /** Performs an Datomic async transaction with multiple operations.
    *
    * {{{
    * Datomic.transact(Seq(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * )).map{ tx =>
    *     ...
    * }
    * }}}
    *
    * @param ops a sequence of [[Operation]]
    * @param connection the implicit [[Connection]]
    * @param ex the implicit scala.concurrent.ExecutionContext
    * @return A future of Transaction Report
    *
    */
  def transact(ops: Seq[Operation])(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = connection.transact(ops)

  /** Performs an Datomic async transaction with single operation.
    *
    * {{{
    * Datomic.transact(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   )
    * )).map{ tx =>
    *     ...
    * }
    * }}}
    * @param op the [[Operation]]
    * @param connection the implicit [[Connection]]
    * @param ex the implicit scala.concurrent.ExecutionContext
    * @return A future of Transaction Report
    */
  def transact(op: Operation)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op))

  /** Performs an Datomic async transaction with multiple operations.
    *
    * {{{
    * Datomic.transact(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * ).map{ tx =>
    *     ...
    * }
    * }}}
    *
    * @param op 1st [[Operation]]
    * @param ops Other [[Operation]]s
    * @param connection the implicit [[Connection]]
    * @param ex the implicit scala.concurrent.ExecutionContext
    * @return A future of Transaction Report
    */
  def transact(op: Operation, ops: Operation*)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op) ++ ops)

  /** Applies a sequence of operations to current database without applying the transaction.
    *
    * It's is as if the data was applied in a transaction but the database is unaffected.
    * This is the same as Java `database.with(...)` taking into account `with` is a reserved word in Scala.
    *
    * Please note the result is not asynchronous Future as the operations is applied at the Peer level
    *
    * {{{
    * Datomic.withData(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "agreede" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * ).map{ tx =>
    *   // Peer database contains the known data + toto + tata
    * }
    * }}}
    *
    */
  //def withData(ops: Seq[Operation])(implicit connection: Connection): TxReport = connection.database.withData(ops)
}
