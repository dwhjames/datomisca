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

import scala.collection.JavaConverters._

trait Operation extends Nativeable

trait DataFunction extends Operation {
  def func: Keyword
}

case class AddFact(fact: Fact) extends DataFunction with Identified {
  override val func = AddFact.kw
  override val id = fact.id

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
}

object AddFact {
  val kw = Keyword("add", Some(Namespace.DB))
  def apply( id: DId, attr: Keyword, value: DatomicData) = new AddFact(Fact(id, attr, value))
}


case class RetractFact(fact: Fact) extends DataFunction with Identified {
  override val func = RetractFact.kw
  override val id = fact.id

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
}

object RetractFact {
  val kw = Keyword("retract", Some(Namespace.DB))
  def apply( id: DId, attr: Keyword, value: DatomicData) = new RetractFact(Fact(id, attr, value))
}

case class RetractEntity(entId: DLong) extends DataFunction {
  override val func = RetractEntity.kw

  def toNative: AnyRef =
    datomic.Util.list(func.toNative, entId.toNative)

  //override def toString = toNative.toString
}

object RetractEntity {
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

case class AddEntity(id: DId, partialProps: Map[Keyword, DatomicData]) extends PartialAddEntity with Operation with Identified {
  override def props = partialProps + (Keyword("id", Namespace.DB) -> id)

  def toNative: AnyRef = {
    import scala.collection.JavaConverters._
    props.map{case (k, v) => (k.toNative, v.toNative)}.asJava
  }

  override def toString = props.map{ case (kw, dd) => kw.toString + " " + dd.toString }.mkString("{\n", "\n  ", "\n}")
}

object AddEntity {
  //def apply(id: DId, props: Map[Keyword, DatomicData]): AddToEntity = new AddToEntity(props + (Keyword("id", Namespace.DB) -> id) )
  def apply(id: DId)(props: (Keyword, DatomicData)*): AddEntity = new AddEntity(id, props.toMap)
  def apply(id: DId, partial: PartialAddEntity) = new AddEntity(id, partial.props)
}

case class AddIdent(val ident: Keyword, partition: Partition = Partition.USER) extends Operation with Identified with Referenceable {
  override lazy val id = DId(partition)
  override lazy val ref = DRef(ident)

  def toNative = AddFact( Fact(id, Keyword("ident", Namespace.DB), ref) ).toNative

  override def toString = toNative.toString

}


