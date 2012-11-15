package reactivedatomic

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import scala.concurrent.Future


case class TxResult(
  dbBefore: datomic.db.Db, 
  dbAfter: datomic.db.Db, 
  txData: Seq[datomic.db.Datum] = Seq(), 
  tempids: Map[Long with datomic.db.DbId, Long] = Map()
) {
  def resolve(id: DId)(implicit db: DDatabase): Option[DLong] = 
    tempids.get(db.value.entid(id.toNative).asInstanceOf[Long with datomic.db.DbId]).map(DLong(_))

  def resolve(identified: Identified)(implicit db: DDatabase): Option[DLong] = resolve(identified.id)

  def resolve(ids: Seq[DId])(implicit db: DDatabase): Seq[Option[DLong]] = 
    ids.map{ id =>
      tempids.get(db.value.entid(id.toNative).asInstanceOf[Long with datomic.db.DbId]).map(DLong(_))
    }

  def resolve(id1: DId, id2: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2) )

  def resolve(id1: Identified, id2: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id) )

  def resolve(id1: DId, id2: DId, id3: DId)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1), resolve(id2), resolve(id3) )

  def resolve(id1: Identified, id2: Identified, id3: Identified)(implicit db: DDatabase): (Option[DLong], Option[DLong], Option[DLong]) = 
    ( resolve(id1.id), resolve(id2.id), resolve(id3.id) )

}

trait Connection {

  def connection: datomic.Connection

  def database: datomic.Database = connection.db()

  def transact(ops: Seq[Operation])(implicit ex: ExecutionContext with Executor): Future[TxResult] = {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val future = Utils.bridgeDatomicFuture(connection.transactAsync(datomicOps))
    
    future.flatMap{ javaMap: java.util.Map[_, _] =>
      val m: Map[Any, Any] = javaMap.toMap.map( t => (t._1.toString, t._2) ) 

      val opt = for( 
        dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]];
        dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]];
        txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.db.Datum]]];
        tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[Long with datomic.db.DbId, Long]]]
      ) yield Future(TxResult(dbBefore, dbAfter, txData.toSeq, tempids.toMap))
    
      opt.getOrElse(Future.failed(new RuntimeException("couldn't parse TxResult")))
    }
  }

  def transact(op: Operation)(implicit ex: ExecutionContext with Executor): Future[TxResult] = transact(Seq(op))
  def transact(op: Operation, ops: Operation *)(implicit ex: ExecutionContext with Executor): Future[TxResult] = transact(Seq(op) ++ ops)
}

object Connection {
  def apply(conn: datomic.Connection) = new Connection {
    def connection = conn
  }
}