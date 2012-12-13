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

package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import scala.reflect.macros.Context
import scala.reflect.internal.util.{Position, OffsetPosition}
import scala.tools.reflect.Eval
import language.experimental.macros

import scala.util.{Try, Success, Failure}

/** Regroup basiic functions provided by Datomic Peer
  * The most important is `connect(uri: String)` which allows to get a Connection on Datomic Peer.
  * As you can see, `database` requires 
  */
trait DatomicPeer {
  /** Builds a Connection from URI
    * In order to benefit from Datomic facilities based on implicit Connection,
    * you should put a connection in an implicit val in your scope. Else, you 
    * can also use provide Connection explicitly.
    *
    * @param uri The URI of Datomic DB
    * @return Connection
    * {{{
    * implicit val conn = Datomic.connection("datomic:mem://mem")
    * }}}
    */
  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    Connection(conn)
  }

  implicit def database(implicit conn: Connection) = conn.database

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)
  def deleteDatabase(uri: String): Boolean = datomic.Peer.deleteDatabase(uri)
  def renameDatabase(uri: String, newName: String): Boolean = datomic.Peer.renameDatabase(uri, newName)
}

trait DatomicTransactor {
  def transact(ops: Seq[Operation])(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = connection.transact(ops)
  def transact(op: Operation)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op))
  def transact(op: Operation, ops: Operation*)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op) ++ ops)  

  def withData(ops: Seq[Operation])(implicit connection: Connection): TxReport = connection.database.withData(ops)
}

trait DatomicFacilities {
  // implicit converters to simplify conversion from Scala Types to Datomic Types
  implicit def toDWrapper[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DWrapper = DWrapperImpl(toDatomic(t)(ddw))

  def add(id: DId)(prop: (Keyword, DWrapper)) = Add(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)
  def add(id: DLong)(prop: (Keyword, DWrapper)) = Add(DId(id), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)
  def add(id: Long)(prop: (Keyword, DWrapper)) = Add(DId(DLong(id)), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  def retract(id: DId)(prop: (Keyword, DWrapper)) = Retract(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  def retractEntity(id: DLong) = RetractEntity(id)
  def retractEntity(id: FinalId) = RetractEntity(DLong(id.underlying))

  def addToEntity(id: DId)(props: (Keyword, DWrapper)*) = 
    AddToEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ): _*)

  def addToEntity(id: DId, props: Map[Keyword, DatomicData]) = AddToEntity(id, props)

  def partialAddToEntity(props: (Keyword, DWrapper)*) = 
    PartialAddToEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ).toMap)

  def addPartition(partition: Partition) = 
    AddToEntity(DId(Partition.DB))(
      Namespace.DB / "ident" -> DString(partition.toString),
      Namespace.DB.INSTALL / "_partition" -> DString("db.part/db")
    )

  def dset(dw: DWrapper*) = DSet(dw.map{t: DWrapper => t.asInstanceOf[DWrapperImpl].underlying}.toSet)

  def toDatomic[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DatomicData = ddw.write(t)
  def fromDatomic[T] = new {
    def apply[DD <: DatomicData](dd: DD)(implicit ddr: DDReader[DD, T]): T = ddr.read(dd)
  }

}

object Datomic 
  extends DatomicPeer 
  with DatomicTransactor 
  with DatomicFacilities 
  with DatomicDataImplicits 
  with ArgsImplicits 
  with DatomicQuery {

  def pureQuery(q: String): PureQuery = macro DatomicQueryMacro.pureQueryImpl

  def typedQuery[A <: Args, B <: Args](q: String): TypedQuery[A, B] = macro DatomicQueryMacro.typedQueryImpl[A, B]

  def rules(q: String): DRuleAliases = macro DatomicQueryMacro.rulesImpl

  def KW(q: String): Keyword = macro DatomicQueryMacro.KWImpl

  def addToEntity(q: String): AddToEntity = macro DatomicMacroOps.addToEntityImpl

  def ops(q: String): Seq[Operation] = macro DatomicMacroOps.opsImpl

  def parseOps(q: String): Try[Seq[Operation]] = DatomicParser.parseOpSafe(q) match {
    case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
      Failure(new RuntimeException(s"Couldn't parse operations[msg:$msg, line:$offsetLine, col:$offsetCol]"))
    case Right(ops) => 
      Success(ops)
  }
}


