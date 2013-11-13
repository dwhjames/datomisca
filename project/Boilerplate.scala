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

import sbt._

object Boilerplate {

  def genCore(dir: File) = {

    val typedQueries = dir / "datomisca" / "typedQueries.scala"
    IO.write(typedQueries, genTypedQueries)

    val typedQueryExecutor = dir / "datomisca" / "TypedQueryExecutor.scala"
    IO.write(typedQueryExecutor, genTypedQueryExecutor)

    val queryResultToTupleInstances = dir / "datomisca" / "QueryResultToTupleInstances.scala"
    IO.write(queryResultToTupleInstances, genQueryResultToTupleInstances)

    val typedAddDbFunctions = dir / "datomisca" / "typedAddDbFunction.scala"
    IO.write(typedAddDbFunctions, genTypedAddDbFunctions)

    val addTxFunctions = dir / "datomisca" / "AddTxFunctionGen.scala"
    IO.write(addTxFunctions, genAddTxFunctions)

    val invokeTxFunctions = dir / "datomisca" / "InvokeTxFunctionGen.scala"
    IO.write(invokeTxFunctions, genInvokeTxFunctions)

    Seq(
      typedQueries, typedQueryExecutor, queryResultToTupleInstances,
      typedAddDbFunctions, addTxFunctions, invokeTxFunctions
    )
  }

  def genExtras(dir: File) = {
    val builders = dir / "datomisca" / "functional" / "builders.scala"
    IO.write(builders, genBuilders)

    Seq(builders)
  }

  def genHeader = {
    ("""|/*
        | * Copyright 2012 Pellucid and Zenexity
        | *
        | * Licensed under the Apache License, Version 2.0 (the "License");
        | * you may not use this file except in compliance with the License.
        | * You may obtain a copy of the License at
        | *
        | *   http://www.apache.org/licenses/LICENSE-2.0
        | *
        | * Unless required by applicable law or agreed to in writing, software
        | * distributed under the License is distributed on an "AS IS" BASIS,
        | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        | * See the License for the specific language governing permissions and
        | * limitations under the License.
        | */
        |
        |package datomisca
        |""").stripMargin
  }

  def genTypedQueries = {
    def genInstance(arity: Int) = {
      val typeParams = ((1 to arity) map (n => "In"+n)).mkString(", ")

      ("""|
          |final class TypedQuery"""+arity+"["+typeParams+""", Out](q: clojure.lang.IPersistentMap) extends AbstractQuery(q)
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|package gen
        |
        |final class TypedQuery0[R](q: clojure.lang.IPersistentMap) extends AbstractQuery(q)
        | """ +
         instances + """
        |""").stripMargin
  }

  def genTypedQueryExecutor = {
    def genInstance(arity: Int) = {
      val typeParamsWithContext = ((1 to arity) map (n => "In"+n+" : ToDatomicCast")).mkString(", ")
      val typeParams = Seq.fill(arity)("_").mkString(", ") // ((1 to arity) map (n => "In"+n)).mkString(", ")
      val queryParams = ((1 to arity) map (n => "in"+n+": In"+n)).mkString(", ")
      val queryArgs = ((1 to arity) map (n => "implicitly[ToDatomicCast[In"+n+"]].to(in"+n+")")).mkString(", ")

      ("""|
          |  def q["""+typeParamsWithContext+""", Out : QueryResultToTuple]
          |       (query: TypedQuery"""+arity+"["+typeParams+", Out], "+queryParams+""")
          |       : Iterable[Out] =
          |    QueryExecutor.execute[Out](query, Seq("""+queryArgs+"""))
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |import gen._
        |
        |private[datomisca] trait TypedQueryExecutor {
        |
        |  def q[In : ToDatomicCast, Out : QueryResultToTuple]
        |       (query: TypedQuery0[Out], dataSource: In)
        |       : Iterable[Out] =
        |    QueryExecutor.execute[Out](query, Seq(implicitly[ToDatomicCast[In]].to(dataSource)))
        |""" +
           instances + """
        |}
        |""").stripMargin
  }

  def genQueryResultToTupleInstances = {
    def genInstance(arity: Int) = {
      val typeParams = Seq.fill(arity)("Any").mkString("(", ", ", ")")
      val body = ((0 until arity) map (n => "DatomicData.toScala(l.get("+n+"))")).mkString("(", ", ", ")")

      ("""|
          |  implicit object QueryResultToTuple"""+arity+" extends QueryResultToTuple["+typeParams+"""] {
          |    override def toTuple(l: java.util.List[AnyRef]): """+typeParams+""" =
          |      """+body+"""
          |  }
          |""").stripMargin
    }

    val instances = ((2 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |private[datomisca] trait QueryResultToTupleInstances {
        |  implicit object QueryResultToTuple1 extends QueryResultToTuple[Any] {
        |    override def toTuple(l: java.util.List[AnyRef]) = DatomicData.toScala(l.get(0))
        |  }""" +
           instances +
     """|}
        |""").stripMargin
  }

  def genTypedAddDbFunctions = {
    def genInstance(arity: Int) = {
      val typeParams = ((1 to arity) map (n => "In"+n+":ToDatomicCast")).mkString(", ")

      ("""|
          |class TypedAddDbFunction"""+arity+"["+typeParams+"""](fn: AddDbFunction) extends TypedAddDbFunction(fn)
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|package gen
        |
        |class TypedAddDbFunction0(fn: AddDbFunction) extends TypedAddDbFunction(fn)
        | """ +
         instances + """
        |""").stripMargin
  }

  def genAddTxFunctions = {
    def genInstance(arity: Int) = {
      val methodTypeParams = ((1 until arity) map (n => "In"+n+":ToDatomicCast")).mkString("[", ", ", "]")
      val params = ((1 to arity) map (n => "p"+n+": String")).mkString("(", ", ", ")")
      val classTypeParams = ((1 until arity) map (n => "In"+n)).mkString("[", ", ", "]")
      val args = ((1 to arity) map (n => "p"+n)).mkString("(", ", ", ")")

      ("""|
          |  def typed"""+methodTypeParams+"""
          |           (kw: Keyword)
          |           """+params+"""
          |           (lang: String, partition: Partition = Partition.USER, imports: String = "", requires: String = "")
          |           (code: String) =
          |    new TypedAddDbFunction"""+(arity-1)+classTypeParams+"""(
          |      new AddDbFunction(kw, lang, Seq"""+args+""", code, imports, requires, partition))
          |""").stripMargin
    }

    val instances = ((2 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |import gen._
        |
        |private[datomisca] trait AddTxFunctionGen {""" +
           instances + """
        |}
        |""").stripMargin
  }

  def genInvokeTxFunctions = {
    def genInstance(arity: Int) = {
      val methodTypeParams = ((1 to arity) map (n => "In"+n+":ToDatomicCast")).mkString("[", ", ", "]")
      val classTypeParams = ((1 to arity) map (n => "In"+n)).mkString("[", ", ", "]")
      val methodParams = ((1 to arity) map (n => "in"+n+": In"+n)).mkString("(", ", ", ")")
      val args = ((1 to arity) map (n => "Datomic.toDatomic(in"+n+")")).mkString("Seq(", ", ", ")")

      ("""|
          |  def apply"""+methodTypeParams+"""
          |           (fn: TypedAddDbFunction"""+arity+classTypeParams+""")
          |           """+methodParams+""" =
          |    new InvokeTxFunction(
          |      fn.ident,
          |      """+args+"""
          |    )
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |import gen._
        |
        |private[datomisca] trait InvokeTxFunctionGen {""" +
           instances + """
          
        |}
        |""").stripMargin
  }

  def genBuilders = {
    def genNesting(arity: Int): String = arity match {
      case 2 => "new ~(a1, a2)"
      case n if n > 2 => "new ~("+genNesting(n-1)+", a"+arity+")"
      case n if n < 2 => throw new RuntimeException
    }

    def genInstance(arity: Int) = {
      val typeList = ((1 to arity) map (n => "A"+n)) // A1 A2 ... An
      val typeListDec = typeList.init // A1 A2 ... An-1
      val typeParams = typeList.mkString("[", ", ", "]") // [A1, A2, ... An]
      val combParams = typeList.mkString("~") // A1~A2~... An
      val combDec = typeListDec.mkString("[", " ~ ", "]") // [A1 ~ A2 ~ ... An-1]
      val params = typeList.mkString("(", ", ", ")") // (A1, A2, ... An)
      val typeParamInc = "[A"+(arity+1)+"]" // [An+1]
      val valList = ((1 to arity) map (n => "a"+n)) // a1 a2 ... an
      val combVals = valList.mkString(" ~ ") // a1 ~ a2 ~ ... an
      val parenVals = valList.mkString("(", ", ", ")") // (a1, a2, ... an)
      val valsWithTypes = ((1 to arity) map (n => "a"+n+": A"+n)).mkString("(", ", ", ")") // (a1: A1, a2: A2, ... an: A2)
      val valsWithIdx = ((1 to arity) map (n => "a._"+n)).mkString("(", ", ", ")") // (a._1, a._2, ... a._n)

      ("""|
          |  class Builder"""+arity+typeParams+"(m1: M"+combDec+", m2:M[A"+arity+"""]) {
          |    def ~"""+typeParamInc+"(m3: M"+typeParamInc+") = new Builder"+(arity+1)+"""(combi(m1, m2), m3)
          |    def and"""+typeParamInc+"(m3: M"+typeParamInc+""") = this.~(m3)
          |
          |    def apply[B](f: """+params+""" => B)(implicit functor: Functor[M]): M[B] =
          |      functor.fmap["""+combParams+", B](combi(m1, m2), { case "+combVals+" => f"+parenVals+""" })
          |
          |    def apply[B](f: B => """+params+""")(implicit functor: ContraFunctor[M]): M[B] =
          |      functor.contramap["""+combParams+", B](combi(m1, m2), { (b: B) => f(b) match { case "+parenVals+" => "+genNesting(arity)+""" } })
          |
          |    def tupled(implicit v: Variant[M]): M["""+params+"""] = (v: @unchecked) match {
          |      case f: Functor[M] => apply("""+valsWithTypes+" => "+parenVals+""")(f)
          |      case f: ContraFunctor[M] => apply((a: """+params+") => "+valsWithIdx+""")(f)
          |    }
          |  }
          |""").stripMargin
    }

    val size = 20 // long compile for 21 and out of mem for 22

    val instances = ((2 until size) map genInstance).mkString

    val combSize = ((1 until size) map (n => "A"+n)).mkString("[", " ~ ", "]")
    val typeParamsSize = ((1 to size) map (n => "A"+n)).mkString("[", ", ", "]")

    genHeader +
    ("""|
        |package functional
        |
        |import scala.language.higherKinds
        |
        |class Builder[M[_]](combi: Combinator[M]) {""" +
           instances + """
        |
        |final case class Builder"""+size+typeParamsSize+"(m1: M"+combSize+", m2: M[A"+size+"""])
        |}
        |""").stripMargin
  }

}
