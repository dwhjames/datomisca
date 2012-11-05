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

  def typedQueryImpl[A <: Args : c.WeakTypeTag, B <: Args : c.WeakTypeTag](c: Context)(q: c.Expr[String]) : c.Expr[TypedQuery[A, B]] = {
      def verifyInputs(query: Query): Option[PositionFailure] = {
        val tpe = implicitly[c.WeakTypeTag[A]].tpe
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
        val tpe = implicitly[c.WeakTypeTag[B]].tpe
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
              /*val enclosingPos = c.enclosingPosition.asInstanceOf[scala.reflect.internal.util.Position]

              val enclosingOffset = 
                enclosingPos.source.lineToOffset(enclosingPos.line - 1 + offsetLine - 1 ) + offsetCol - 1

              val offsetPos = new OffsetPosition(enclosingPos.source, enclosingOffset)*/
              val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]
              println("treePos:"+treePos)
              val offsetPos = new OffsetPosition(
                treePos.source, 
                computeOffset(treePos, offsetLine, offsetCol)
              )

              println("offsetPos:"+offsetPos)
              c.abort(offsetPos.asInstanceOf[c.Position], msg)

            case Right(t) => c.Expr[TypedQuery[A, B]]( inc.incept(TypedQuery[A, B](t)) )
          }

        case _ => c.abort(c.enclosingPosition, "Only accepts String")
      }
      
    }

  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  implicit val DStringWrites = DWrites[String]( (s: String) => DString(s) )
  implicit val DIntWrites = DWrites[Int]( (i: Int) => DInt(i) )
  implicit val DLongWrites = DWrites[Long]( (l: Long) => DLong(l) )
  implicit val DBooleanWrites = DWrites[Boolean]( (b: Boolean) => DBoolean(b) )
  implicit val DFloatWrites = DWrites[Float]( (b: Float) => DFloat(b) )
  implicit val DDoubleWrites = DWrites[Double]( (b: Double) => DDouble(b) )
  implicit val DReferenceable = DWrites[Referenceable]( (ref: Referenceable) => ref.ident )
  implicit val DDatomicData = DWrites[DatomicData]( (dd: DatomicData) => dd )
  implicit def DSeqWrites[T : DWrites] = DWrites[Traversable[T]]( (l: Traversable[T]) => DSeq(l.map(toDatomic(_)).toSeq) )

  // implicit converters
  implicit def toDWrapper[T : DWrites](t: T): DWrapper = DWrapperImpl(toDatomic(t))

  def addEntity(id: DId)(props: (Keyword, DWrapper)*) = 
    AddEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ): _*)

  def addEntity(props: (Keyword, DWrapper)*) = 
    AddEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ).toMap)

  def dseq(dw: DWrapper*) = DSeq(dw.map(t => t.asInstanceOf[DWrapperImpl].value))

  def toDatomic[T : DWrites](t: T): DatomicData = implicitly[DWrites[T]].write(t)

  /*implicit class KeywordInterpolation(val sc: StringContext) extends AnyVal {
    def KW(args: Any*): Keyword = {
      if(args.length > 0 || sc.parts.length > 1) sys.error("Keyword interpolation can't contact variables")
      else DatomicParser.parseKeyword(sc.parts(0))
    }
  }*/ 

  def KW(q: String): Keyword = macro KWImpl

  def KWImpl(c: Context)(q: c.Expr[String]) : c.Expr[Keyword] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseKeywordSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(kw) => c.Expr[Keyword]( inc.incept(kw) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
    
  }

  def transac(ops: String): Future[TxResult] = macro transacImpl

  def transacImpl(c: Context)(ops: c.Expr[String]): c.Expr[Future[TxResult]] = {
    import c.universe._

    val inc = inception(c)

    ops.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseAddEntityParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = ops.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ae) => 
            c.Expr[Future[TxResult]](Apply(
              Select(Ident(newTermName("connection")), "transact"),
              List(
                inc.incept(ae)
              )
            ))
            //c.Expr[AddEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }

  /*implicit class EntityHelper(val sc: StringContext) extends AnyVal {
    def entity(args: Any*): String = {
      sc.s(args.map(a => a match {
        case ref: Referenceable => ref.ident.toString
        case id: Identified => id.id.toString
        case dd: DatomicData => dd.toString
      }): _*)
    }
  }*/
}

trait DWrites[-T] {
  def write(t: T): DatomicData
}

object DWrites{
  def apply[T](f: T => DatomicData) = new DWrites[T] {
    def write(t: T) = f(t)
  }
}

trait DWrapper extends NotNull
private[reactivedatomic] case class DWrapperImpl(value: DatomicData) extends DWrapper



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

