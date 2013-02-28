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

package datomisca

import scala.language.higherKinds
import language.experimental.macros

import scala.util.{Try, Success, Failure}
import scala.util.parsing.input.Positional

import dmacros._

trait DatomicQueryExecutor extends QueryExecutorPure with QueryExecutorInOut with QueryExecutorAuto with ArgsImplicits

/* DATOMIC QUERY */
trait Query {
  def find: Find
  def wizz: Option[With] = None
  def in: Option[In] = None
  def where: Where

  override def toString = s"""[ $find ${wizz.map( _.toString + " " ).getOrElse("")}${in.map( _.toString + " " ).getOrElse("")}$where ]"""
}

object Query extends QueryMacros {
  def apply(find: Find, where: Where): PureQuery = PureQuery(find, None, None, where)
  def apply(find: Find, in: In, where: Where): PureQuery = PureQuery(find, None, Some(in), where)
  def apply(find: Find, in: Option[In], where: Where): PureQuery = PureQuery(find, None, in, where)
  def apply(find: Find, wizz: With, in: In, where: Where): PureQuery = PureQuery(find, Some(wizz), Some(in), where)
  def apply(find: Find, wizz: Option[With], in: Option[In], where: Where): PureQuery = PureQuery(find, wizz, in, where)
  //def apply[In <: Args, Out <: Args](q: PureQuery): TypedQuery[In, Out] = TypedQuery[In, Out](q)
}


trait QueryMacros {
  /** Creates a macro-based compile-time pure query from a String (only syntax validation is performed).<br/>
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    * 
    *     - A [[PureQuery]] is the low-level query AST provided in the Scala API.
    *     - [[TypedQuery]] is based on it.
    *     - When a [[PureQuery]] is executed, it returns a `List[List[DatomicData]].
    *     - All returned types are [[DatomicData]].
    * 
    * {{{
    * // creates a query
    * val q = Datomic.Query.pure("""
    *  [ 
    *    :find ?e ?name
    *    :in $ ?char
    *    :where  [ ?e :person/name ?name ] 
    *            [ ?e :person/character ?char ]
    *  ]
    * """)
    *
    * Datomic.query(q, database, person.character / "violent") map {
    *   case List(DLong(e), DString(name)) =>
    *     ...
    * }
    * }}}
    *
    * @param q the query String
    * @return a PureQuery
    */
  def pure(q: String): PureQuery = macro DatomicQueryMacro.pureQueryImpl

  /** Creates a macro-based compile-time typed query from a String:
    *    - syntax validation is performed.
    *    - determines number of input(M) parameters
    *    - determines number of output(N) parameters
    *    - creates a TypedQueryN[DatomicData, ...M-times, TupleN[DatomicData, ...N-times]]
    *
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    * 
    * When a [[TypedQuery]] is executed, it returns a `List[TupleN[DatomicData, DatomicData, ...]]` where X corresponds
    * to the number of output parameters
    *
    * {{{
    * val q = Datomic.Query.auto(""" 
    *   [
    *    :find ?e ?name ?age
    *    :in $ [[?name ?age]]
    *    :where [?e :person/name ?name]
    *           [?e :person/age ?age]
    *   ]
    * """)
    *
    * Datomic.q(
    *   q, database, 
    *   DSet(
    *     DSet(DString("toto"), DLong(30L)),
    *     DSet(DString("tutu"), DLong(54L))
    *   )
    * ) map {
    *   case (DLong(e), DString(n), DLong(a)) => 
    *      ...
    * }    
    * }}}
    */
  def auto(q: String) = macro DatomicQueryMacro.autoTypedQueryImpl
  def apply(q: String) = macro DatomicQueryMacro.autoTypedQueryImpl
  
  /** Creates a macro-based compile-time typed query from a String:
    *    - syntax validation is performed.
    *    - type validation (for the time being, number of input/output args, later much more)
    *
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    * 
    * A ``TypedQuery[InArgs, OutArgs] takes 2 type parameters:
    *     - InArgs <: Args defining the number of input args
    *     - OutArgs <: args defining the number of output args
    * 
    * When a [[TypedQuery]] is executed, it returns a `List[TupleX[DatomicData, DatomicData, ...]]` where X corresponds
    * to the number of output parameters
    *
    * {{{
    * val q = Datomic.Query.manual[Args2, Args3](""" 
    *   [
    *    :find ?e ?name ?age
    *    :in $ [[?name ?age]]
    *    :where [?e :person/name ?name]
    *           [?e :person/age ?age]
    *   ]
    * """)
    *
    * Datomic.q(
    *   q, database, 
    *   DSet(
    *     DSet(DString("toto"), DLong(30L)),
    *     DSet(DString("tutu"), DLong(54L))
    *   )
    * ) map {
    *   case (DLong(e), DString(n), DLong(a)) => 
    *      ...
    * }    
    * }}}
    */
  def manual[A <: Args, B <: Args](q: String): TypedQueryInOut[A, B] = macro DatomicQueryMacro.typedQueryImpl[A, B]    


  //def typedQuery[A <: Args, B <: Args](q: String): TypedQuery[A, B] = macro DatomicQueryMacro.typedQueryImpl[A, B]

  /** Macro-based helper to create Rule alias to be used in Queries.
    * {{{
    * val totoRule = Datomic.Query.rules("""
    *   [[[toto ?e]
    *     [?e :person/name "toto"]
    *   ]]
    * """)
    *
    * val q = Datomic.typedQuery[Args2, Args2]("""
    *   [
    *     :find ?e ?age
    *     :in $ %
    *     :where [?e :person/age ?age]
    *            (toto ?e)
    *   ]
    * """)
    *
    * Datomic.query(q, database, totoRule) map {
    *   case (DLong(e), DLong(age)) => 
    *     ...
    * }
    * }}}
    */
  def rules(q: String): DRuleAliases = macro DatomicQueryMacro.rulesImpl
}

case class PureQuery(override val find: Find, override val wizz: Option[With] = None, override val in: Option[In] = None, override val where: Where) extends Query {
  self =>

  private[datomisca] def prepare[InArgs <: Args](in: InArgs)(implicit db: DDatabase): List[List[DatomicData]] = {
    import scala.collection.JavaConversions._

    val qser = self.toString
    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(db.toNative)
      else args
    }

    //println("QSER:"+qser+ " - args:"+args)
    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    results.map { fields =>
      fields.map { field => Datomic.toDatomicData(field) }
    }    
  }
  
}

case class TypedQueryInOut[InArgs <: Args, OutArgs <: Args](query: PureQuery) extends Query {
  self => 
  override def find = query.find
  override def wizz = query.wizz
  override def in = query.in
  override def where = query.where

  private[datomisca] def prepare[T]()(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T], tf: ToFunction[InArgs, List[T]]) = {
    new DatomicExecutor {
      type F[_] = tf.F[List[T]]
      def execute = tf.convert(
        ((in: InArgs) => QueryExecutor.directQueryInOut(self, in)).andThen((t: List[OutArgs]) => t.map( out => ott.convert(out)) )
      )
    }
  }
}

abstract class TypedQueryAuto(query: PureQuery) extends Query {
  override def find = query.find
  override def wizz = query.wizz
  override def in = query.in
  override def where = query.where
}

case class TypedQueryAuto0[R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto1[A, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto2[A, B, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto3[A, B, C, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto4[A, B, C, D, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto5[A, B, C, D, E, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto6[A, B, C, D, E, F, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto7[A, B, C, D, E, F, G, R](query: PureQuery) extends TypedQueryAuto(query)
case class TypedQueryAuto8[A, B, C, D, E, F, G, H, R](query: PureQuery) extends TypedQueryAuto(query)


/* DATOMIC QUERY */
object QueryExecutor {
  private[datomisca] def directQuery[InArgs <: Args](q: Query, in: InArgs)(implicit db: DDatabase): List[List[DatomicData]] = {
    import scala.collection.JavaConversions._

    // serializes query
    val qser = q.toString

    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(db.toNative)
      else args
    }

    //println("QSER:"+qser+ " - args:"+args)
    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    results.map { fields =>
      fields.map { field => Datomic.toDatomicData(field) }
    }    
  }

  private[datomisca] def directQueryInOut[InArgs <: Args, OutArgs <: Args](q: Query, in: InArgs)(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs]): List[OutArgs] = {
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._

    // serializes query
    val qser = q.toString

    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(db.toNative)
      else args
    }

    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    val listOfTry = results.map { fields =>
      outConv.toArgs(fields.map { field => Datomic.toDatomicData(field) })
    }

    listOfTry.foldLeft(Nil: List[OutArgs]){ (acc, e) => acc :+ e }
  }

  private[datomisca] def directQueryOut[OutArgs](q: Query, in: Seq[Object])(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs]): List[OutArgs] = {
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._

    // serializes query
    val qser = q.toString

    //println("QSER:"+qser)

    val args = {
      if(in.isEmpty) Seq(db.toNative)
      else in
    }

    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    val listOfTry = results.map { fields =>
      outConv.toArgs(fields.map { field => Datomic.toDatomicData(field) })
    }

    listOfTry.foldLeft(Nil: List[OutArgs]){ (acc, e) => acc :+ e }
  }
}

trait QueryExecutorPure {
  def q[InArgs <: Args](query: PureQuery, in: InArgs = Args0())(implicit db: DDatabase) = 
    QueryExecutor.directQuery(query, in)(db)
}

trait QueryExecutorInOut extends DatomicQueryHidden {
  @deprecated("use this one only on purpose", "")
  def q[OutArgs <: Args, T](query: TypedQueryInOut[Args0, OutArgs])(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = query.prepare[T]()(db, outConv, ott, ArgsImplicits.toF0[List[T]]).execute()

  // .. others are in DatomicQueryHidden
}

trait QueryExecutorAuto extends DDWriterImplicits{
  def q[R](query: TypedQueryAuto0[R])(
    implicit outConv: DatomicDataToArgs[R], db: DDatabase
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq())(db, outConv)

  def q[R](query: TypedQueryAuto0[R], db: DDatabase)(
    implicit outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq())(db, outConv)

  def q[A, R](query: TypedQueryAuto1[A, R], a: A)(
    implicit db: DDatabase, 
             ddwa: DDWriter[DatomicData, A],
             outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq(ddwa.write(a).toNative))(db, outConv)

  def q[A, B, R](query: TypedQueryAuto2[A, B, R], a: A, b: B)(
    implicit db: DDatabase, 
             ddwa: DDWriter[DatomicData, A], ddwb: DDWriter[DatomicData, B], 
             outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq(ddwa.write(a).toNative, ddwb.write(b).toNative))(db, outConv)

  def q[A, B, C, R](query: TypedQueryAuto3[A, B, C, R], a: A, b: B, c: C)(
    implicit db: DDatabase, 
             ddwa: DDWriter[DatomicData, A], ddwb: DDWriter[DatomicData, B], ddwc: DDWriter[DatomicData, C], 
             outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq(ddwa.write(a).toNative, ddwb.write(b).toNative, ddwc.write(c).toNative))(db, outConv)

  def q[A, B, C, D, R](query: TypedQueryAuto4[A, B, C, D, R], a: A, b: B, c: C, d:D)(
    implicit db: DDatabase, 
             ddwa: DDWriter[DatomicData, A], ddwb: DDWriter[DatomicData, B], ddwc: DDWriter[DatomicData, C], ddwd: DDWriter[DatomicData, D], 
             outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq(ddwa.write(a).toNative, ddwb.write(b).toNative, ddwc.write(c).toNative, ddwd.write(d).toNative))(db, outConv)

  def q[A, B, C, D, E, R](query: TypedQueryAuto5[A, B, C, D, E, R], a: A, b: B, c: C, d:D, e:E)(
    implicit db: DDatabase, 
             ddwa: DDWriter[DatomicData, A], ddwb: DDWriter[DatomicData, B], ddwc: DDWriter[DatomicData, C], ddwd: DDWriter[DatomicData, D], ddwe: DDWriter[DatomicData, E], 
             outConv: DatomicDataToArgs[R]
  ): List[R] = QueryExecutor.directQueryOut[R](query, Seq(ddwa.write(a).toNative, ddwb.write(b).toNative, ddwc.write(c).toNative, ddwd.write(d).toNative, ddwe.write(e).toNative))(db, outConv)

}


/**
 * Converts a function In => Out into another function 
 * that returns Out but takes other input parameters 
 * built from In
 */
trait ToFunction[In <: Args, Out] {
  type F[Out]
  def convert(from: (In => Out)): F[Out]
}

/**
 * Convert Args into a Tuple
 */
trait ArgsToTuple[A <: Args, T] {
  def convert(from: A): T
}

/**
 * Converts a Seq[DatomicData] into an Args with potential error (exception)
 */
trait DatomicDataToArgs[T] {
  def toArgs(l: Seq[DatomicData]): T
}

trait DatomicExecutor {
  type F[_]
  def execute: F[_]
}

object ArgsImplicits extends ArgsImplicits

trait ArgsImplicits extends ToFunctionImplicits with DatomicDataToArgsImplicits with ArgsToTupleImplicits

trait ToFunctionImplicits extends ToFunctionImplicitsHidden {
  implicit def toF0[Out] = new ToFunction[Args0, Out] {
    type F[Out] = Function0[Out]
    def convert(f: (Args0 => Out)): F[Out] = () => f(Args0())
  }
}

trait DatomicDataToArgsImplicits extends DatomicDataToArgsImplicitsHidden {

  implicit object DatomicDataToArgs1 extends DatomicDataToArgs[Args1] {
    def toArgs(l: Seq[DatomicData]): Args1 = l match {
      case List(_1) => Args1(_1)
      case _ => throw new RuntimeException("Could convert Seq to Args1")
    }
  }

}

trait ArgsToTupleImplicits extends ArgsToTupleImplicitsHidden {

  implicit def Args1ToTuple = new ArgsToTuple[Args1, DatomicData] {
    def convert(from: Args1) = from._1
  }

}


sealed trait Args {
  def toSeq: Seq[Object]
}

case class Args0() extends Args {
  override def toSeq: Seq[Object] = Seq()
}

case class Args1(_1: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative)
}

case class Args2(_1: DatomicData, _2: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative)
}

case class Args3(_1: DatomicData, _2: DatomicData, _3: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative)
}

case class Args4(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative)
}

case class Args5(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative)
}

case class Args6(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative)
}

case class Args7(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative)
}

case class Args8(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative)
}

case class Args9(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative)
}

case class Args10(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative)
}

case class Args11(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative)
}

case class Args12(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative)
}

case class Args13(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative)
}

case class Args14(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative)
}

case class Args15(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative)
}

case class Args16(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative)
}

case class Args17(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative)
}

case class Args18(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData, _18: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative, _18.toNative)
}

case class Args19(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData, _18: DatomicData, _19: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative, _18.toNative, _19.toNative)
}

case class Args20(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData, _18: DatomicData, _19: DatomicData, _20: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative, _18.toNative, _19.toNative, _20.toNative)
}

case class Args21(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData, _18: DatomicData, _19: DatomicData, _20: DatomicData, _21: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative, _18.toNative, _19.toNative, _20.toNative, _21.toNative)
}

case class Args22(_1: DatomicData, _2: DatomicData, _3: DatomicData, _4: DatomicData, _5: DatomicData, _6: DatomicData, _7: DatomicData, _8: DatomicData, _9: DatomicData, _10: DatomicData, _11: DatomicData, _12: DatomicData, _13: DatomicData, _14: DatomicData, _15: DatomicData, _16: DatomicData, _17: DatomicData, _18: DatomicData, _19: DatomicData, _20: DatomicData, _21: DatomicData, _22: DatomicData) extends Args {
  override def toSeq: Seq[Object] = Seq(_1.toNative, _2.toNative, _3.toNative, _4.toNative, _5.toNative, _6.toNative, _7.toNative, _8.toNative, _9.toNative, _10.toNative, _11.toNative, _12.toNative, _13.toNative, _14.toNative, _15.toNative, _16.toNative, _17.toNative, _18.toNative, _19.toNative, _20.toNative, _21.toNative, _22.toNative)
}
