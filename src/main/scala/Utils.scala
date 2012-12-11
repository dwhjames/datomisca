package reactivedatomic

import scala.concurrent.{Future, Promise}
import datomic.ListenableFuture

import scala.concurrent.ExecutionContext
import java.util.concurrent.Executor
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.util.{Try, Success, Failure}

object Utils {
  def bridgeDatomicFuture[T](listenF: ListenableFuture[T])(implicit ex: ExecutionContext): Future[T] = {
    val p = Promise[T]

    listenF.addListener(
      new java.lang.Runnable {
        override def run: Unit = {
          p.complete(
            try {
              Success(listenF.get(0, MILLISECONDS))
            }catch {
              case e => Failure(e)
            }
          )
        }
      },
      new java.util.concurrent.Executor {
        def execute(arg0: Runnable): Unit = ex.execute(arg0)
      }
    )

    p.future
  }

  import scala.collection.generic.CanBuildFrom
  import scala.collection.TraversableLike

  def sequence[A, M[_]](l: M[Try[A]])
    (implicit toTraversableLike: M[Try[A]] => TraversableLike[Try[A], M[Try[A]]], 
    cbf: CanBuildFrom[M[_], A, M[A]]): Try[M[A]] = {
    l.foldLeft(Success(cbf()): Try[scala.collection.mutable.Builder[A, M[A]]]){ (acc, e) => e match {
      case Failure(e) => Failure(e)
      case Success(s) => acc.map{ acc => acc += s }
    }}.map(_.result)
  }

  /** Converts a java.util.Map[_,_] returns by connection.transact into a TxReport 
    * It requires an implicit DDatabase because it must resolve the keyword from Datom (from Integer in the map)
    */
  def toTxReport(javaMap: java.util.Map[_, _])(implicit database: DDatabase): TxReport = {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    val m: Map[Any, Any] = javaMap.toMap.map( t => (t._1.toString, t._2) ) 

    val opt = for{
      dbBefore <- m.get(datomic.Connection.DB_BEFORE.toString).asInstanceOf[Option[datomic.db.Db]].map( DDatabase(_) ).orElse(None)
      dbAfter <- m.get(datomic.Connection.DB_AFTER.toString).asInstanceOf[Option[datomic.db.Db]].map( DDatabase(_) ).orElse(None)
      txData <- m.get(datomic.Connection.TX_DATA.toString).asInstanceOf[Option[java.util.List[datomic.Datom]]].orElse(None)
      tempids <- m.get(datomic.Connection.TEMPIDS.toString).asInstanceOf[Option[java.util.Map[Long with datomic.db.DbId, Long]]].orElse(None)
    } yield TxReport(dbBefore, dbAfter, txData.map(DDatom(_)(database)).toSeq, tempids.toMap)

    opt.get
  }

  def queue2Stream[A](queue: java.util.concurrent.BlockingQueue[A]): Stream[Option[A]] = {
    def toStream: Stream[Option[A]] = {
      Option(queue.poll()) match {
        case None => Stream.cons(None, toStream)
        case Some(a) => Stream.cons(Some(a), toStream)
      }
    }

    toStream
  }

}

trait TxReportQueue {
  implicit def database: DDatabase

  def queue: java.util.concurrent.BlockingQueue[java.util.Map[_, _]]

  lazy val stream: Stream[Option[TxReport]] = Utils.queue2Stream[java.util.Map[_, _]](queue).map{ 
    case None => None
    case Some(javaMap) => Some(Utils.toTxReport(javaMap)(database))
  }
}

sealed class <:!<[A, B] extends NotNull

trait LowerPriorityImplicits {
  /** do not call explicitly! */
  implicit def sub[A, B >: A]: <:!<[A, B] = sys.error("should not be called")
}
object <:!< extends LowerPriorityImplicits {
  /** do not call explicitly! */
  implicit def nsub[A, B]: <:!<[A, B] = new <:!<[A, B]
}

/**
 * Combination operator
 */
case class ~[A,B](_1:A, _2:B)

trait Variant[M[_]]

trait Functor[M[_]] extends Variant[M] {
  def fmap[A, B](ma: M[A], f: A => B): M[B]
}

trait ContraFunctor[M[_]] extends Variant[M] {
  def contramap[A, B](ma: M[A], f: B => A): M[B]
}

class FunctorOps[M[_],A](ma: M[A])(implicit fu: Functor[M]){
  def fmap[B](f: A => B): M[B] = fu.fmap(ma, f)
}

class ContraFunctorOps[M[_],A](ma:M[A])(implicit fu:ContraFunctor[M]){
  def contramap[B](f: B => A): M[B] = fu.contramap(ma, f)
}


/**
 * Combinator base trait
 */
trait Combinator[M[_]] {
  def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B]
}

class CombinatorOps[A, M[_]](ma: M[A])(implicit combi: Combinator[M]) {
  def ~[B](mb: M[B]) = {
    val builder = new Builder(combi)
    new builder.Builder2(ma, mb)
  }
  def and[B](mb: M[B]) = this.~(mb)
}


class Builder[M[_]](combi: Combinator[M]) {
  class Builder2[A1, A2](m1: M[A1], m2: M[A2]) {
    def ~[A3](m3: M[A3]) = Builder3(combi(m1, m2), m3)
    def and[A3](m3: M[A3]) = this.~(m3)

    def apply[B](f: (A1, A2) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2, B]( combi(m1, m2), { case a1 ~ a2 => f(a1, a2) } )

    def apply[B](f: B => (A1, A2))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2) => new ~(a1, a2) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2) => (a1, a2) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2) => (a._1, a._2) }(f)
    }
  }

  case class Builder3[A1, A2, A3](m1: M[A1 ~ A2], m2: M[A3]) {
    def ~[A4](m3: M[A4]) = Builder4(combi(m1, m2), m3)
    def and[A4](m3: M[A4]) = this.~(m3)

    def apply[B](f: (A1, A2, A3) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 => f(a1, a2, a3) } )

    def apply[B](f: B => (A1, A2, A3))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3) => new ~(new ~(a1, a2), a3) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3) => (a1, a2, a3) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3) => (a._1, a._2, a._3) }(f)
    }
  }

  case class Builder4[A1, A2, A3, A4](m1: M[A1 ~ A2 ~ A3], m2: M[A4]) {
    def ~[A5](m3: M[A5]) = Builder5(combi(m1, m2), m3)
    def and[A5](m3: M[A5]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 => f(a1, a2, a3, a4) } )

    def apply[B](f: B => (A1, A2, A3, A4))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4) => new ~(new ~(new ~(a1, a2), a3), a4) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4) => (a1, a2, a3, a4) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4) => (a._1, a._2, a._3, a._4) }(f)
    }
  }

  case class Builder5[A1, A2, A3, A4, A5](m1: M[A1 ~ A2 ~ A3 ~ A4], m2: M[A5]) {
    def ~[A6](m3: M[A6]) = Builder6(combi(m1, m2), m3)
    def and[A6](m3: M[A6]) = this.~(m3)

    def apply[B](f: (A1, A2, A3, A4, A5) => B)(implicit functor: Functor[M]): M[B] = 
      functor.fmap[A1~A2~A3~A4~A5, B]( combi(m1, m2), { case a1 ~ a2 ~ a3 ~ a4 ~ a5 => f(a1, a2, a3, a4, a5) } )

    def apply[B](f: B => (A1, A2, A3, A4, A5))(implicit functor: ContraFunctor[M]): M[B] = 
      functor.contramap[A1~A2~A3~A4~A5, B]( combi(m1, m2), { b:B => f(b) match { case (a1, a2, a3, a4, a5) => new ~(new ~(new ~(new ~(a1, a2), a3), a4), a5) } } )

    def tupled(implicit v: Variant[M]): M[(A1, A2, A3, A4, A5)] = (v: @unchecked) match {
      case f: Functor[M] => apply{ (a1: A1, a2: A2, a3: A3, a4: A4, a5: A5) => (a1, a2, a3, a4, a5) }(f)
      case f: ContraFunctor[M] => apply{ a: (A1, A2, A3, A4, A5) => (a._1, a._2, a._3, a._4, a._5) }(f)
    }
  }

  case class Builder6[A1, A2, A3, A4, A5, A6](m1: M[A1 ~ A2 ~ A3 ~ A4 ~ A5], m2: M[A6])

}

trait Monad[M[_]] {
  def unit[A](a: A): M[A]
  def bind[A, B](ma: M[A], f: A => M[B]): M[B]
}

object CombinatorImplicits extends CombinatorImplicits

trait CombinatorImplicits {
  def unlift[A, B](f: A => Option[B]): A => B = Function.unlift(f)

  implicit def CombinatorOpsWrapper[A, M[_]](ma: M[A])(implicit combi: Combinator[M]) = new CombinatorOps(ma)(combi)

  implicit def CombinatorWrapper[M[_]](implicit monad: Monad[M]) = new Combinator[M] {
    def apply[A, B](ma: M[A], mb: M[B]): M[A ~ B] = monad.bind(ma, (a: A) => monad.bind(mb, (b: B) => monad.unit(new ~(a, b)) ))
  }

  implicit def toFunctorOps[M[_], A](ma: M[A])(implicit fu: Functor[M]): FunctorOps[M, A] = new FunctorOps(ma)
  implicit def toContraFunctorOps[M[_], A](ma: M[A])(implicit fu: ContraFunctor[M]): ContraFunctorOps[M, A] = new ContraFunctorOps(ma)

}