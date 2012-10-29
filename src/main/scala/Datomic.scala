package reactivedatomic

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Try, Success, Failure}
import language.experimental.macros
import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}

object Datomic extends ArgsImplicits with DatomicCompiler{

  implicit def connection(implicit uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  implicit def database(implicit conn: Connection) = DDatabase(conn.database)


  /*def q(s: String)(implicit db: DDatabase): ResultSet = {
    import scala.collection.JavaConversions._

    val qast = DatomicParser.parseQuery(s)
    val qser = qast.toString

    ResultSet(
      datomic.Peer.q(qser, db.value).toList.map(_.toList)
    )
  }*/

  def pureQuery(q: String): PureQuery = macro pureQueryImpl

  def pureQueryImpl(c: Context)(q: c.Expr[String]) : c.Expr[PureQuery] = {
      import c.universe._

      val inc = inception(c)

      q.tree match {
        case Literal(Constant(s: String)) => 
          DatomicParser.parseQuerySafe(s) match {
            case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
              val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset = 
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)
            case Right(q) => c.Expr[PureQuery]( inc.incept(q) )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
      }
    
  }

  def query[A <: Args, B <: Args](q: String): TypedQuery[A, B] = macro typedQueryImpl[A, B]

  def typedQueryImpl[A <: Args : c.AbsTypeTag, B <: Args : c.AbsTypeTag](c: Context)(q: c.Expr[String]) : c.Expr[TypedQuery[A, B]] = {
      //println(implicitly[c.AbsTypeTag[T]].tpe <:< implicitly[c.TypeTag[Boolean]].tpe)
      //println(c.universe.TypeTag.unapply(implicitly[c.AbsTypeTag[T]].tpe))

      def verifyInputs(query: Query): Option[PositionFailure] = {
        val tpe = implicitly[c.AbsTypeTag[A]].tpe
        val sz = query.in.map( _.inputs.size ).getOrElse(0)
        lazy val argPos = c.macroApplication.children(0).children(1).pos

        query.in.map{ in => 
          if(
            (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2) 
            || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
          )
            Some(PositionFailure("Query Error in \":in\" : Expected %d INPUT variables".format(sz), 1, argPos.column))
          else None
        } 
        .getOrElse(None)
        
      }

      def verifyOutputs(query: Query): Option[PositionFailure] = {
        val tpe = implicitly[c.AbsTypeTag[B]].tpe
        val sz = query.find.outputs.size
        val argPos = c.macroApplication.children(0).children(2).pos

        if(
          (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2)
          || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
        )
          Some(PositionFailure("Query Error in \":find\" : Expected %d OUTPUT variables".format(sz), 1, argPos.column))
        else None
      }

      def verifyTypes(query: Query): Option[PositionFailure] = {
        verifyInputs(query).flatMap( _ => 
          verifyOutputs(query) 
        )
      }

      import c.universe._

      val inc = inception(c)

      q.tree match {
        case Literal(Constant(s: String)) => 
          DatomicParser.parseQuerySafe(s).right.flatMap{ (t: PureQuery) => verifyTypes(t) match {
            case Some(p: PositionFailure) => Left(p)
            case None => Right(t)
          } } match {
            case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
              val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset = 
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)

            case Right(t) => c.Expr[TypedQuery[A, B]]( inc.incept(TypedQuery[A, B](t))
             )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
      }
      
    }

  //def query(s: String)(implicit db: DDatabase): ResultSet = q(s)

  /*def pureQuery(s: String)(implicit db: DDatabase): List[List[DatomicData]] = {
    import scala.collection.JavaConversions._

    val qast = DatomicParser.parseQuery(s)
    val qser = qast.toString

    val results: List[List[Any]] = datomic.Peer.q(qser, db.value).toList.map(_.toList)
    
    results.map { fields =>
      fields.map { field => DatomicData.toDatomicData(field) }
    }
  }*/

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

}


case class TxResult(dbBefore: datomic.db.Db, dbAfter: datomic.db.Db, txData: Seq[datomic.db.Datum] = Seq(), tempids: Map[datomic.db.DbId, Any] = Map()) 

trait Connection {

  def connection: datomic.Connection

  def database: datomic.Database = connection.db()

  def provisionSchema(schema: Schema): Future[TxResult] = {
    transact(schema.ops)
  }

  def createSchemaOld(schema: exp.Schema): Future[TxResult] = {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    val datomicOps = schema.ops.map( _.asJava ).toList.asJava

    val m: Map[Any, Any] = connection.transact(datomicOps).get().toMap.map( t => (t._1.toString, t._2) )

    //println("MAP:"+m)
    //println("datomic.Connection.DB_BEFORE="+m.get(datomic.Connection.DB_BEFORE.toString))
    val opt = for( 
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]];
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]];
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.db.Datum]]];
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[datomic.db.DbId, Any]]]
    ) yield Future(TxResult(dbBefore, dbAfter, txData.toSeq, tempids.toMap))
    
    opt.getOrElse(Future.failed(new RuntimeException("couldn't parse TxResult")))    
  }

  def transact(ops: Seq[Operation]): Future[TxResult] = {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val javaFut = connection.transact(datomicOps)

    val m: Map[Any, Any] = javaFut.get().toMap.map( t => (t._1.toString, t._2) )

    val opt = for( 
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]];
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]];
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.db.Datum]]];
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[datomic.db.DbId, Any]]]
    ) yield Future(TxResult(dbBefore, dbAfter, txData.toSeq, tempids.toMap))
    
    opt.getOrElse(Future.failed(new RuntimeException("couldn't parse TxResult")))    
  }

  def transact(op: Operation): Future[TxResult] = transact(Seq(op))
  def transact(op: Operation, ops: Operation *): Future[TxResult] = transact(Seq(op) ++ ops)
}

