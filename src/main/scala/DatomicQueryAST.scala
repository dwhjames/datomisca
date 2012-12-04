package reactivedatomic

import scala.util.{Try, Success, Failure}
import scala.util.parsing.input.Positional


/* DATOMIC RULES */
sealed trait Rule extends Positional

/* DATOMIC DATA RULES */
case class DataRule(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty, tx: Term = Empty, added: Term = Empty) extends Rule {
  override def toString = """[%s%s %s %s%s%s]""".format(
    if(ds == ImplicitDS) "" else ds+" ",
    entity,
    attr,
    value,
    if(tx == Empty) "" else (" "+tx),
    if(added == Empty) "" else (" "+added)
  )
}

/* DATOMIC EXPRESSION RULES */
sealed trait Binding
case class ScalarBinding(name: Term) extends Binding {
  override def toString = name.toString
}

case class TupleBinding(names: Seq[Term]) extends Binding {
  override def toString = "[ " + names.map( _.toString ).mkString(" ") + " ]"
}

case class CollectionBinding(name: Term) extends Binding {
  override def toString = "[ " + name.toString + " ... ]" 
}

case class RelationBinding(names: Seq[Term]) extends Binding {
  override def toString = "[[ " + names.map( _.toString ).mkString(" ") + " ]]"
}

case class DFunction(name: String) {
  override def toString = name.toString
}

case class DPredicate(name: String) {
  override def toString = name.toString
}

case class ExpressionRule(expr: Expression) extends Rule {
  override def toString = s"""[ $expr ]"""
}

sealed trait Expression
case class PredicateExpression(predicate: DPredicate, args: Seq[Term]) extends Expression {
  override def toString = s"""($predicate ${args.map( _.toString ).mkString(" ")})"""
}
case class FunctionExpression(function: DFunction, args: Seq[Term], binding: Binding) extends Expression {
  override def toString = s"""($function ${args.map( _.toString ).mkString(" ")}) $binding"""
}

/* RULE ALIAS */
case class RuleAliasCall(name: String, args: Seq[Term]) extends Rule {
  override def toString = """( %s %s )""".format(name, args.map( _.toString ).mkString("", " ", ""))
}

/* WHERE */
case class Where(rules: Seq[Rule]) extends Positional {
  override def toString = rules.map( _.toString ).mkString(":where ", " ", "")
}

/* IN */
case class In(inputs: Seq[Input]) extends Positional {
  override def toString = inputs.map( _.toString ).mkString(":in ", " ", "")
}

sealed trait Input
case class InDataSource(ds: DataSource) extends Input {
  override def toString = ds.toString
}
case class InVariable(binding: Binding) extends Input {
  override def toString = binding.toString
}
case object InRuleAlias extends Input {
  override def toString = "%"
}

/* DATOMIC FIND */
case class Find(outputs: Seq[Output]) extends Positional {
  override def toString = outputs.map( _.toString ).mkString(":find ", " ", "")
}

sealed trait Output
case class OutVariable(variable: Var) extends Output {
  override def toString = variable.toString
}

/* DATOMIC QUERY */
trait Query {
  def find: Find
  def in: Option[In] = None
  def where: Where

  override def toString = s"""[ $find ${in.map( _.toString + " " ).getOrElse("")}$where ]"""

  def apply[InArgs <: Args, OutArgs <: Args](in: InArgs)(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs]): List[OutArgs] = directQuery(in)

  private[reactivedatomic] def directQuery[InArgs <: Args, OutArgs <: Args](in: InArgs)(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs]): List[OutArgs] = {
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._
    val qser = this.toString
    println("in:"+in)
    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(db.toNative)
      else args
    }

    println("QSER:"+qser+ " - args:"+args)

    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    val listOfTry = results.map { fields =>
      outConv.toArgs(fields.map { field => DatomicData.toDatomicData(field) })
    }

    listOfTry.foldLeft(Nil: List[OutArgs]){ (acc, e) => acc :+ e }
  }

}

object Query {
  def apply(find: Find, where: Where): PureQuery = PureQuery(find, None, where)
  def apply(find: Find, in: In, where: Where): PureQuery = PureQuery(find, Some(in), where)
  def apply(find: Find, in: Option[In], where: Where): PureQuery = PureQuery(find, in, where)
  def apply[In <: Args, Out <: Args](q: PureQuery): TypedQuery[In, Out] = TypedQuery[In, Out](q)
}

case class PureQuery(override val find: Find, override val in: Option[In] = None, override val where: Where) extends Query {
  self =>

  private[reactivedatomic] def prepare[InArgs <: Args](in: InArgs)(implicit db: DDatabase): List[List[DatomicData]] = {
    import scala.collection.JavaConversions._

    val qser = self.toString
    val args = {
      val args = in.toSeq
      if(args.isEmpty) Seq(db.toNative)
      else args
    }

    println("QSER:"+qser+ " - args:"+args)
    val results: List[List[Any]] = datomic.Peer.q(qser, args: _*).toList.map(_.toList)
    
    results.map { fields =>
      fields.map { field => DatomicData.toDatomicData(field) }
    }    
  }
  
}

case class TypedQuery[InArgs <: Args, OutArgs <: Args](query: PureQuery) extends Query {
  override def find = query.find
  override def in = query.in
  override def where = query.where

  private[reactivedatomic] def prepare[T]()(implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T], tf: ToFunction[InArgs, List[T]]) = {
    new DatomicExecutor {
      type F[_] = tf.F[List[T]]
      def execute = tf.convert(
        ((in: InArgs) => directQuery(in)).andThen((t: List[OutArgs]) => t.map( out => ott.convert(out)) )
      )
    }
  }
}

trait DatomicQuery {
  def query[InArgs <: Args](q: PureQuery, in: InArgs = Args0())(implicit db: DDatabase) = 
    q.prepare(in)

  def query[OutArgs <: Args, T](q: TypedQuery[Args0, OutArgs])(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF0[List[T]]).execute()

  def query[OutArgs <: Args, T](q: TypedQuery[Args1, OutArgs], d1: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF1[List[T]]).execute(d1)

  def query[OutArgs <: Args, T](q: TypedQuery[Args2, OutArgs], d1: DatomicData, d2: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF2[List[T]]).execute(d1, d2)

  def query[OutArgs <: Args, T](q: TypedQuery[Args3, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF3[List[T]]).execute(d1, d2, d3)

  def query[OutArgs <: Args, T](q: TypedQuery[Args4, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData)(
    implicit db: DDatabase, outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(db, outConv, ott, ArgsImplicits.toF4[List[T]]).execute(d1, d2, d3, d4)
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
trait DatomicDataToArgs[T <: Args] {
  def toArgs(l: Seq[DatomicData]): T
}

trait DatomicExecutor {
  type F[_]
  def execute: F[_]
}

object ArgsImplicits extends ArgsImplicits

trait ArgsImplicits {

  implicit def toF0[Out] = new ToFunction[Args0, Out] {
    type F[Out] = Function0[Out]
    def convert(f: (Args0 => Out)): F[Out] = () => f(Args0())
  }

  implicit def toF1[Out] = new ToFunction[Args1, Out] {
    type F[Out] = Function1[DatomicData, Out]
    def convert(f: (Args1 => Out)): F[Out] = (d1: DatomicData) => f(Args1(d1))
  }

  implicit def toF2[Out] = new ToFunction[Args2, Out] {
    type F[Out] = Function2[DatomicData, DatomicData, Out]
    def convert(f: (Args2 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData) => f(Args2(d1, d2)) 
  }

  implicit def toF3[Out] = new ToFunction[Args3, Out] {
    type F[Out] = Function3[DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args3 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData) => f(Args3(d1, d2, d3))
  }

  implicit def toF4[Out] = new ToFunction[Args4, Out] {
    type F[Out] = Function4[DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args4 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData) => f(Args4(d1, d2, d3, d4))
  }

  implicit object DatomicDataToArgs1 extends DatomicDataToArgs[Args1] {
    def toArgs(l: Seq[DatomicData]): Args1 = l match {
      case List(_1) => Args1(_1)
      case _ => throw new RuntimeException("Could convert Seq to Args1")
    }
  }


  implicit object DatomicDataToArgs2 extends DatomicDataToArgs[Args2] {
    def toArgs(l: Seq[DatomicData]): Args2 = l match {
      case List(_1, _2) => Args2(_1, _2)
      case _ => throw new RuntimeException("Could convert Seq to Args2")
    }
  }

  implicit def DatomicDataToArgs3 = new DatomicDataToArgs[Args3] {
    def toArgs(l: Seq[DatomicData]): Args3 = l match {
      case List(_1, _2, _3) => Args3(_1, _2, _3)
      case _ => throw new RuntimeException("Could convert Seq to Args3")
    }
  }

  implicit def DatomicDataToArgs4 = new DatomicDataToArgs[Args4] {
    def toArgs(l: Seq[DatomicData]): Args4 = l match {
      case List(_1, _2, _3, _4) => Args4(_1, _2, _3, _4)
      case _ => throw new RuntimeException("Could convert Seq to Args4")
    }
  }

  implicit def Args1ToTuple = new ArgsToTuple[Args1, DatomicData] {
    def convert(from: Args1) = from._1
  }

  implicit def Args2ToTuple = new ArgsToTuple[Args2, (DatomicData, DatomicData)] {
    def convert(from: Args2) = (from._1, from._2)
  }

  implicit def Args3ToTuple = new ArgsToTuple[Args3, (DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args3) = (from._1, from._2, from._3)
  }

  implicit def Args4ToTuple = new ArgsToTuple[Args4, (DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args4) = (from._1, from._2, from._3, from._4)
  }

}

