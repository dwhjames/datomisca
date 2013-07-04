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

  def gen(dir: File) = {
    val typedQueryAutos = dir / "datomisca" / "typedQueryAuto.scala"
    IO.write(typedQueryAutos, genTypedQueryAutos)

    val queryExecutorAuto = dir / "datomisca" / "QueryExecutorAuto.scala"
    IO.write(queryExecutorAuto, genQueryExecutorAuto)

    val queryResultToTupleInstances = dir / "datomisca" / "QueryResultToTupleInstances.scala"
    IO.write(queryResultToTupleInstances, genQueryResultToTupleInstances)

    Seq(typedQueryAutos, queryExecutorAuto, queryResultToTupleInstances)
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
        """).stripMargin
  }

  def genTypedQueryAutos = {
    def genInstance(arity: Int) = {
      val typeParams = ((1 to arity) map (n => "In"+n)).mkString(", ")

      ("""|
          |case class TypedQueryAuto"""+arity+"["+typeParams+""", Out](query: PureQuery) extends TypedQueryAuto(query)
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |case class TypedQueryAuto0[R](query: PureQuery) extends TypedQueryAuto(query)
        | """ +
         instances + """
        |""").stripMargin
  }

  def genQueryExecutorAuto = {
    def genInstance(arity: Int) = {
      val typeParams = ((1 to arity) map (n => "In"+n)).mkString(", ")
      val queryParams = ((1 to arity) map (n => "in"+n+": DatomicData")).mkString(", ")
      val queryArgs = ((1 to arity) map (n => "in"+n+".toNative")).mkString(", ")

      ("""|
          |  def q["""+typeParams+""", Out]
          |       (query: TypedQueryAuto"""+arity+"["+typeParams+", Out], "+queryParams+""")
          |       (implicit outConv: QueryResultToTuple[Out])
          |       : Iterable[Out] =
          |    QueryExecutor.directQueryOut[Out](query, Seq("""+queryArgs+"""))
          |""").stripMargin
    }

    val instances = ((1 to 22) map genInstance).mkString

    genHeader +
    ("""|
        |trait QueryExecutorAuto {
        |
        |  def q[Out]
        |       (query: TypedQueryAuto0[Out], dataSource: DatomicData)
        |       (implicit conv: QueryResultToTuple[Out])
        |       : Iterable[Out] =
        |    QueryExecutor.directQueryOut[Out](query, Seq(dataSource.toNative))
        |""" +
           instances + """
        |}
        |""").stripMargin
  }

  def genQueryResultToTupleInstances = {
    def genInstance(arity: Int) = {
      val typeParams = Seq.fill(arity)("DatomicData").mkString("(", ", ", ")")
      val body = ((0 until arity) map (n => "Datomic.toDatomicData(l.get("+n+"))")).mkString("(", ", ", ")")

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
        |trait QueryResultToTupleInstances {
        |  implicit object QueryResultToTuple1 extends QueryResultToTuple[DatomicData] {
        |    override def toTuple(l: java.util.List[AnyRef]) = Datomic.toDatomicData(l.get(0))
        |  }""" +
           instances +
     """|}
        |""").stripMargin
  }

}
