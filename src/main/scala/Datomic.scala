package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Try, Success, Failure}

object Datomic extends ArgsImplicits{

  implicit def connection(implicit uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  implicit def database(implicit conn: Connection) = DDatabase(conn.database)



  /*implicit def tuple2Converter = new DatomicResultConverter[Tuple2] {
    def convert(l: List[Any]) = l match {
      case Nil => None
      case a1 :: a2 :: tail => Some(Tuple2(a1, a2))
    }
  }*/

  def q(s: String)(implicit db: DDatabase): ResultSet = {
    import scala.collection.JavaConversions._

    val qast = DatomicParser.parseQuery(s)
    val qser = DatomicSerializers.querySerialize(qast)

    ResultSet(
      datomic.Peer.q(qser, db.value).toList.map(_.toList)
    )
  }

  def query(s: String)(implicit db: DDatabase): ResultSet = q(s)

  def q2(s: String)(implicit db: DDatabase): List[List[DatomicData]] = {
    import scala.collection.JavaConversions._

    val qast = DatomicParser.parseQuery(s)
    val qser = DatomicSerializers.querySerialize(qast)

    val results: List[List[Any]] = datomic.Peer.q(qser, db.value).toList.map(_.toList)
    
    results.map { fields =>
      fields.map { field => DatomicData.toDatomicData(field) }
    }

  }

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

}



case class TxResult(dbBefore: datomic.db.Db, dbAfter: datomic.db.Db, txData: Seq[datomic.db.Datum] = Seq(), tempids: Map[Any, Any] = Map()) 

trait Connection {
  import scala.collection.JavaConverters._
  import scala.collection.JavaConversions._

  def connection: datomic.Connection

  def database: datomic.Database = connection.db()

  def createSchema(schema: Schema): Future[TxResult] = {
    transact(schema.ops)
  }

  def transact(ops: Seq[Operation]) = {
    val m: Map[Any, Any] = connection.transact(ops.map( _.asJava ).toList.asJava).get().toMap.map( t => (t._1.toString, t._2))

    //println("MAP:"+m)
    //println("datomic.Connection.DB_BEFORE="+m.get(datomic.Connection.DB_BEFORE.toString))
    val opt = for( 
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]];
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]];
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.db.Datum]]];
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[Any, Any]]]
    ) yield Future(TxResult(dbBefore, dbAfter, txData.toSeq, tempids.toMap))
    
    opt.getOrElse(Future.failed(new RuntimeException("couldn't parse TxResult")))    
  }
}


trait Res {
  def values: List[Any]

  def toList = values

}

object Res {
  def apply(vs: List[Any]) = new Res {
    override lazy val values: List[Any] = vs
  }

  def unapplySeq(res: Res): Option[List[Any]] = Some(res.toList)
}


trait ResultSet {

  def results: List[Res]
  def collect[T](pf: PartialFunction[Res, T]): List[T] = results.collect(pf)
  def map[T](f: Res => T): List[T] = results.map(f)

}

object ResultSet {
  def apply(rez: List[List[Any]]) = new ResultSet{
    override lazy val results: List[Res] = rez.map( (values: List[Any]) => Res(values) )
  }
}

