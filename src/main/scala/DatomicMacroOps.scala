package reactivedatomic

import scala.reflect.macros.Context
import scala.reflect.internal.util.{Position, OffsetPosition}

object DatomicMacroOps extends DatomicInception {

  def addToEntityImpl(c: Context)(q: c.Expr[String]): c.Expr[AddToEntity] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseAddToEntityParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ae) => 
            c.Expr[AddToEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }

  def opsImpl(c: Context)(q: c.Expr[String]): c.Expr[Seq[Operation]] = {
    import c.universe._

    val inc = inception(c)

    q.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseOpParsingSafe(s) match {
          case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
            val treePos = q.tree.pos.asInstanceOf[scala.reflect.internal.util.Position]

            val offsetPos = new OffsetPosition(
              treePos.source, 
              computeOffset(treePos, offsetLine, offsetCol)
            )
            c.abort(offsetPos.asInstanceOf[c.Position], msg)
          case Right(ops) => 
            c.Expr[Seq[Operation]]( inc.incept(ops) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }


  /*def transact(ops: String): Future[TxResult] = macro transactImpl

  def transactImpl(c: Context)(ops: c.Expr[String]): c.Expr[Future[TxResult]] = {
    import c.universe._

    val inc = inception(c)

    ops.tree match {
      case Literal(Constant(s: String)) => 
        DatomicParser.parseAddToEntityParsingSafe(s) match {
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
            //c.Expr[AddToEntity]( inc.incept(ae) )
        }

      case _ => c.abort(c.enclosingPosition, "Only accepts String")
    }
  }*/
}