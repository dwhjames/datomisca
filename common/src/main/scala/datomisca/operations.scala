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

import scala.language.reflectiveCalls


trait Operation extends Nativeable

trait DataFunction extends Operation {
  def func: Keyword
}

case class AddFact(id: DId, attr: Keyword, value: DatomicData) extends DataFunction with TempIdentified {
  override val func = Keyword("add", Some(Namespace.DB))
  // override val id = fact.id

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, id.toNative, attr.toNative, value.toNative)
}

case class RetractFact(id: Long, attr: Keyword, value: DatomicData) extends DataFunction with FinalIdentified {
  override val func = Keyword("retract", Some(Namespace.DB))

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, id: java.lang.Long, attr.toNative, value.toNative)
}

case class RetractEntity(id: Long) extends DataFunction with FinalIdentified {
  override val func = RetractEntity.kw

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, id: java.lang.Long)

  //override def toString = toNative.toString
}

object RetractEntity {
  def apply(id: DLong): RetractEntity = RetractEntity(id.underlying)
  val kw = Keyword("retractEntity", Some(Namespace.DB.FN))
}

trait PartialAddEntity {
  def props: Map[Keyword, DatomicData]

  def ++(other: PartialAddEntity) = PartialAddEntity( props ++ other.props )

  def toMap = props
  //override def toString = props.toString
}

object PartialAddEntity {
  def apply(theProps: Map[Keyword, DatomicData]) = new PartialAddEntity {
    def props = theProps
  }

  def empty: PartialAddEntity = apply(Map())
}

case class AddEntity(id: DId, partialProps: Map[Keyword, DatomicData]) extends PartialAddEntity with Operation with TempIdentified {
  override def props = partialProps + (Keyword("id", Namespace.DB) -> id)

  def toNative: AnyRef = {
    import scala.collection.JavaConverters._
    props.map{case (k, v) => (k.toNative, v.toNative)}.asJava
  }

  override def toString = props.map{ case (kw, dd) => kw.toString + " " + dd.toString }.mkString("{\n", "\n  ", "\n}")
}

case class AddIdent(ident: Keyword, partition: Partition = Partition.USER) extends Operation with KeywordIdentified {
  lazy val ref = DRef(ident)
  def toNative = AddFact(DId(partition), Keyword("ident", Namespace.DB), ref).toNative

  override def toString = toNative.toString
}


