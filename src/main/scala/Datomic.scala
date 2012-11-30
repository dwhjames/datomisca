package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future

import scala.util.{Try, Success, Failure}
import language.experimental.macros
import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}
import scala.concurrent.ExecutionContext

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

  implicit def database(implicit conn: Connection) = DDatabase(conn.database)

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)
  def deleteDatabase(uri: String): Boolean = datomic.Peer.deleteDatabase(uri)
  def renameDatabase(uri: String, newName: String): Boolean = datomic.Peer.renameDatabase(uri, newName)
}

trait DatomicTransactor {
  def transact(ops: Seq[Operation])(implicit connection: Connection, ex: ExecutionContext): Future[TxResult] = connection.transact(ops)
  def transact(op: Operation)(implicit connection: Connection, ex: ExecutionContext): Future[TxResult] = transact(Seq(op))
  def transact(op: Operation, ops: Operation*)(implicit connection: Connection, ex: ExecutionContext): Future[TxResult] = transact(Seq(op) ++ ops)  
}

trait DatomicFacilities {
  // implicit converters to simplify conversion from Scala Types to Datomic Types
  implicit def toDWrapper[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DWrapper = DWrapperImpl(toDatomic(t)(ddw))

  def addEntity(id: DId)(props: (Keyword, DWrapper)*) = 
    AddEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ): _*)

  def partialAddEntity(props: (Keyword, DWrapper)*) = 
    PartialAddEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ).toMap)

  def dset(dw: DWrapper*) = DSet(dw.map{t: DWrapper => t.asInstanceOf[DWrapperImpl].value}.toSet)

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

  def KW(q: String): Keyword = macro DatomicQueryMacro.KWImpl

  def addEntity(q: String): AddEntity = macro DatomicMacroOps.addEntityImpl

  def ops(q: String): Seq[Operation] = macro DatomicMacroOps.opsImpl

  def parseOps(q: String): Try[Seq[Operation]] = DatomicParser.parseOpSafe(q) match {
    case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
      Failure(new RuntimeException(s"Couldn't parse operations[msg:$msg, line:$offsetLine, col:$offsetCol]"))
    case Right(ops) => 
      Success(ops)
  }
}


