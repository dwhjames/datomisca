package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Datomic {

  implicit def connection(implicit uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  implicit def database(implicit conn: Connection) = conn.database

  def q(s: String)(implicit db: datomic.Database): ResultSet = {
    import scala.collection.JavaConversions._

    val qast = DatomicParser.parseQuery(s)
    val qser = DatomicSerializers.querySerialize(qast)

    ResultSet(
      datomic.Peer.q(qser, db).toList.map(_.toList)
    )
  }

  def query(s: String)(implicit db: datomic.Database): ResultSet = q(s)

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
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