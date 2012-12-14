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

package reactivedatomic

trait Identified {
  def id: DId
}

trait Referenceable {
  def ident: DRef
}

case class Fact(id: DId, attr: Keyword, value: DatomicData)

case class Partition(keyword: Keyword) {
  override def toString = keyword.toString
}

object Partition {
  val DB = Partition(Keyword("db", Some(Namespace.DB.PART)))
  val TX = Partition(Keyword("tx", Some(Namespace.DB.PART)))
  val USER = Partition(Keyword("user", Some(Namespace.DB.PART)))
}

trait Operation extends Nativeable

trait DataFunction extends Operation {
  def func: Keyword
}

case class Add(fact: Fact) extends DataFunction with Identified {
  override val func = Add.kw
  override val id = fact.id

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  } 
}

object Add {
  val kw = Keyword("add", Some(Namespace.DB))
  def apply( id: DId, attr: Keyword, value: DatomicData) = new Add(Fact(id, attr, value))
}


case class Retract(fact: Fact) extends DataFunction with Identified {
  override val func = Retract.kw
  override val id = fact.id

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, fact.id.toNative, fact.attr.toNative, fact.value.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  }
}

object Retract {
  val kw = Keyword("retract", Some(Namespace.DB))
  def apply( id: DId, attr: Keyword, value: DatomicData) = new Retract(Fact(id, attr, value))
}

case class RetractEntity(entId: DLong) extends DataFunction {
  override val func = RetractEntity.kw

  def toNative: java.lang.Object = {
    val l = List[java.lang.Object]( func.toNative, entId.toNative)
    val javal = new java.util.ArrayList[Object]()

    l.foreach( e => javal.add(e.asInstanceOf[Object]) )
    javal
  } 

  //override def toString = toNative.toString
}

object RetractEntity {
  val kw = Keyword("retractEntity", Some(Namespace.DB.FN))
}

trait PartialAddToEntity {
  def props: Map[Keyword, DatomicData]

  def ++(other: PartialAddToEntity) = PartialAddToEntity( props ++ other.props )

  //override def toString = props.toString
}

object PartialAddToEntity {
  def apply(theProps: Map[Keyword, DatomicData]) = new PartialAddToEntity {
    def props = theProps
  }

  def empty: PartialAddToEntity = apply(Map())
}

case class AddToEntity(id: DId, partialProps: Map[Keyword, DatomicData]) 
extends PartialAddToEntity with Operation with Identified {
  override def props = partialProps + (Keyword("id", Namespace.DB) -> id)

  def toNative: java.lang.Object = {
    import scala.collection.JavaConverters._
    ( props.map( t => (t._1.toNative, t._2.toNative) ) + (Keyword("id", Namespace.DB).toNative -> id.toNative) ).asJava
  }

  override def toString = props.map{ case (kw, dd) => kw.toString + " " + dd.toString }.mkString("{\n", "\n  ", "\n}")
}

object AddToEntity {
  //def apply(id: DId, props: Map[Keyword, DatomicData]): AddToEntity = new AddToEntity(props + (Keyword("id", Namespace.DB) -> id) )
  def apply(id: DId)(props: (Keyword, DatomicData)*): AddToEntity = new AddToEntity(id, props.toMap)
  def apply(id: DId, partial: PartialAddToEntity) = new AddToEntity(id, partial.props)
}

case class AddIdent(override val ident: DRef, partition: Partition = Partition.USER) extends Operation with Identified with Referenceable {
  override lazy val id = DId(partition)

  def toNative = Add( Fact(id, Keyword("ident", Namespace.DB), ident) ).toNative

  override def toString = toNative.toString

}

object AddIdent {
  def apply(ident: Keyword) = new AddIdent(DRef(ident))
  def apply(ident: Keyword, partition: Partition) = new AddIdent(DRef(ident), partition)
}



