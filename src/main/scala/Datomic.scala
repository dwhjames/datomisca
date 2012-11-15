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
import java.util.concurrent.Executor


object Datomic extends ArgsImplicits with DatomicCompiler with DatomicDataImplicits {

  implicit def connection(implicit uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    Connection(conn)
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
        query.in.flatMap{ in => 
          if(
            (tpe <:< implicitly[c.TypeTag[Args2]].tpe && sz != 2) 
            || (tpe <:< implicitly[c.TypeTag[Args3]].tpe && sz != 3)
          ) {
            Some(PositionFailure("Query Error in \":in\" : Expected %d INPUT variables".format(sz), 1, argPos.column))
          }
          else None
        }
        
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
        verifyInputs(query) match {
          case Some(p) => Some(p)
          case None => verifyOutputs(query) 
        }
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
  def deleteDatabase(uri: String): Boolean = datomic.Peer.deleteDatabase(uri)

  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    new Connection {
      def connection = conn
    }
  }

  // implicit converters
  implicit def toDWrapper[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DWrapper = DWrapperImpl(toDatomic(t)(ddw))

  def addEntity(id: DId)(props: (Keyword, DWrapper)*) = 
    AddEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ): _*)

  def partialAddEntity(props: (Keyword, DWrapper)*) = 
    PartialAddEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].value) ).toMap)

  def addEntity(ops: String) = macro addEntityImpl

  def dset(dw: DWrapper*) = DSet(dw.map{t: DWrapper => t.asInstanceOf[DWrapperImpl].value}.toSet)

  def toDatomic[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DatomicData = ddw.write(t)

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


  def addEntityImpl(c: Context)(ops: c.Expr[String]): c.Expr[AddEntity] = {
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
            c.Expr[AddEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }

  def transact(ops: Seq[Operation])(implicit connection: Connection, ex: ExecutionContext with Executor): Future[TxResult] = connection.transact(ops)
  def transact(op: Operation)(implicit connection: Connection, ex: ExecutionContext with Executor): Future[TxResult] = transact(Seq(op))
  def transact(op: Operation, ops: Operation*)(implicit connection: Connection, ex: ExecutionContext with Executor): Future[TxResult] = transact(Seq(op) ++ ops)

  /*def transact(ops: String): Future[TxResult] = macro transactImpl

  def transactImpl(c: Context)(ops: c.Expr[String]): c.Expr[Future[TxResult]] = {
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
  }*/


}


