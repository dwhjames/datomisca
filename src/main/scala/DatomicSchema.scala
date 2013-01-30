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

trait SchemaType[DD <: DatomicData] {
  def keyword: Keyword
}

case object SchemaTypeString extends SchemaType[DString] {
  def keyword = Keyword(Namespace.DB.TYPE, "string")
}

case object SchemaTypeBoolean extends SchemaType[DBoolean] {
  def keyword = Keyword(Namespace.DB.TYPE, "boolean")
}

case object SchemaTypeLong extends SchemaType[DLong] {
  def keyword = Keyword(Namespace.DB.TYPE, "long")
}

case object SchemaTypeBigInt extends SchemaType[DBigInt] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigint")
}

case object SchemaTypeFloat extends SchemaType[DFloat] {
  def keyword = Keyword(Namespace.DB.TYPE, "float")
}

case object SchemaTypeDouble extends SchemaType[DDouble] {
  def keyword = Keyword(Namespace.DB.TYPE, "double")
}

case object SchemaTypeBigDec extends SchemaType[DBigDec] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigdec")
}

case object SchemaTypeRef extends SchemaType[DRef] {
  def keyword = Keyword(Namespace.DB.TYPE, "ref")
}

case object SchemaTypeInstant extends SchemaType[DInstant] {
  def keyword = Keyword(Namespace.DB.TYPE, "instant")
}

case object SchemaTypeUuid extends SchemaType[DUuid] {
  def keyword = Keyword(Namespace.DB.TYPE, "uuid")
}

case object SchemaTypeUri extends SchemaType[DUri] {
  def keyword = Keyword(Namespace.DB.TYPE, "uri")
}

case object SchemaTypeBytes extends SchemaType[DBytes] {
  def keyword = Keyword(Namespace.DB.TYPE, "bytes")
}

//case class SchemaType(keyword: Keyword)

object SchemaType {
  val string = SchemaTypeString //SchemaType(Keyword(Namespace.DB.TYPE, "string"))
  val boolean = SchemaTypeBoolean //SchemaType(Keyword(Namespace.DB.TYPE, "boolean"))
  val long = SchemaTypeLong //SchemaType(Keyword(Namespace.DB.TYPE, "long"))
  val bigint = SchemaTypeBigInt //SchemaType(Keyword(Namespace.DB.TYPE, "bigint"))
  val float = SchemaTypeFloat //SchemaType(Keyword(Namespace.DB.TYPE, "float"))
  val double = SchemaTypeDouble //SchemaType(Keyword(Namespace.DB.TYPE, "double"))
  val bigdec = SchemaTypeBigDec //SchemaType(Keyword(Namespace.DB.TYPE, "bigdec"))
  val ref = SchemaTypeRef //SchemaType(Keyword(Namespace.DB.TYPE, "ref"))
  val instant = SchemaTypeInstant //SchemaType(Keyword(Namespace.DB.TYPE, "instant"))
  val uuid = SchemaTypeUuid //SchemaType(Keyword(Namespace.DB.TYPE, "uuid"))
  val uri = SchemaTypeUri //SchemaType(Keyword(Namespace.DB.TYPE, "uri"))
  val bytes = SchemaTypeBytes //SchemaType(Keyword(Namespace.DB.TYPE, "bytes"))
}

trait Cardinality {
  def keyword: Keyword
}

case object CardinalityOne extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "one")
}

case object CardinalityMany extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "many")
}

//case class Cardinality(keyword: Keyword)

object Cardinality {
  val one = CardinalityOne //Cardinality(Keyword(Namespace.DB.CARDINALITY, "one"))
  val many = CardinalityMany //Cardinality(Keyword(Namespace.DB.CARDINALITY, "many"))
}

case class Unique(keyword: Keyword)

object Unique {
  val value = Unique(Keyword(Namespace.DB.UNIQUE, "value"))
  val identity = Unique(Keyword(Namespace.DB.UNIQUE, "identity"))
}

sealed trait Attribute[DD <: DatomicData, Card <: Cardinality] extends Operation with Identified with Term with Namespaceable {
  def ident: Keyword
  def valueType: SchemaType[DD]
  def cardinality: Card
  def doc: Option[String] = None
  def unique: Option[Unique] = None
  def index: Option[Boolean] = None
  def fulltext: Option[Boolean] = None
  def isComponent: Option[Boolean] = None
  def noHistory: Option[Boolean] = None

  // using partiton :db.part/db
  override lazy val id = DId(Partition.DB)
  override lazy val name = ident.name
  override lazy val ns = ident.ns

  lazy val toAddOps: AddEntity = {
    val mb = new scala.collection.mutable.MapBuilder[Keyword, DatomicData, Map[Keyword, DatomicData]](Map(
      Attribute.id -> id,
      Attribute.ident -> DRef(ident),
      Attribute.valueType -> DRef(valueType.keyword),
      Attribute.cardinality -> DRef(cardinality.keyword)
    ))
    if(doc.isDefined) mb += Attribute.doc -> DString(doc.get)
    if(unique.isDefined) mb += Attribute.unique -> DRef(unique.get.keyword)
    if(index.isDefined) mb += Attribute.index -> DBoolean(index.get)
    if(fulltext.isDefined) mb += Attribute.fulltext -> DBoolean(fulltext.get)
    if(isComponent.isDefined) mb += Attribute.isComponent -> DBoolean(isComponent.get)
    if(noHistory.isDefined) mb += Attribute.noHistory -> DBoolean(noHistory.get)
    
    // installing attribute
    mb += Attribute.installAttr -> DRef(Partition.DB.keyword)

    AddEntity(id, mb.result())
  }
  
  override def toNative: AnyRef = toAddOps.toNative
  override def toString = ident.toString

  def stringify = s"""
{ 
  ${Attribute.id} $id
  ${Attribute.ident} ${DRef(ident)}
  ${Attribute.valueType} ${DRef(valueType.keyword)}
  ${Attribute.cardinality} ${DRef(cardinality.keyword)}""" +
  ( if(doc.isDefined) { "\n  " + Attribute.doc + " " + DString(doc.get) } else { "" } ) +
  ( if(unique.isDefined) { "\n  " + Attribute.unique + " " + DRef(unique.get.keyword) } else { "" } ) +
  ( if(index.isDefined) { "\n  " + Attribute.index + " " + DBoolean(index.get) } else { "" }) +
  ( if(fulltext.isDefined) { "\n  " + Attribute.fulltext + " " + DBoolean(fulltext.get) } else { "" }) +
  ( if(isComponent.isDefined) { "\n  " + Attribute.isComponent + " " + DBoolean(isComponent.get) } else { "" }) +
  ( if(noHistory.isDefined) { "\n  " + Attribute.noHistory + " " + DBoolean(noHistory.get) } else { "" }) +
  "\n  " + Attribute.installAttr + " " + DRef(Partition.DB.keyword) + 
  "\n}"

} 

object Attribute {
  def apply[DD <: DatomicData, Card <: Cardinality](
    ident: Keyword,
    valueType: SchemaType[DD],
    cardinality: Card,
    doc: Option[String] = None,
    unique: Option[Unique] = None,
    index: Option[Boolean] = None,
    fulltext: Option[Boolean] = None,
    isComponent: Option[Boolean] = None,
    noHistory: Option[Boolean] = None
  ) = new RawAttribute(ident, valueType, cardinality, doc, unique, index, fulltext, isComponent, noHistory)

  val id = Keyword(Namespace.DB, "id")
  val ident = Keyword(Namespace.DB, "ident")
  val valueType = Keyword(Namespace.DB, "valueType")
  val cardinality = Keyword(Namespace.DB, "cardinality")
  val doc = Keyword(Namespace.DB, "doc")
  val unique = Keyword(Namespace.DB, "unique")
  val index = Keyword(Namespace.DB, "index")
  val fulltext = Keyword(Namespace.DB, "fulltext")
  val isComponent = Keyword(Namespace.DB, "isComponent")
  val noHistory = Keyword(Namespace.DB, "noHistory")
  val installAttr = Keyword(Namespace.DB.INSTALL, "_attribute")
}

case class RawAttribute[DD <: DatomicData, Card <: Cardinality](
  override val ident: Keyword,
  override val valueType: SchemaType[DD],
  override val cardinality: Card,
  override val doc: Option[String] = None,
  override val unique: Option[Unique] = None,
  override val index: Option[Boolean] = None,
  override val fulltext: Option[Boolean] = None,
  override val isComponent: Option[Boolean] = None,
  override val noHistory: Option[Boolean] = None
) extends Attribute[DD, Card] {
  def withDoc(str: String) = copy( doc = Some(str) )
  def withUnique(u: Unique) = copy( unique = Some(u) )
  def withIndex(b: Boolean) = copy( index = Some(b) )
  def withFullText(b: Boolean) = copy( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy( noHistory = Some(b) )

}



case class RefAttribute[T](
  override val ident: Keyword,
  override val doc: Option[String] = None,
  override val unique: Option[Unique] = None,
  override val index: Option[Boolean] = None,
  override val fulltext: Option[Boolean] = None,
  override val isComponent: Option[Boolean] = None,
  override val noHistory: Option[Boolean] = None
) extends Attribute[DRef, CardinalityOne.type]{
  
  override val valueType = SchemaType.ref
  override val cardinality = CardinalityOne

  def withDoc(str: String) = copy[T]( doc = Some(str) )
  def withUnique(u: Unique) = copy[T]( unique = Some(u) )
  def withIndex(b: Boolean) = copy[T]( index = Some(b) )
  def withFullText(b: Boolean) = copy[T]( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy[T]( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy[T]( noHistory = Some(b) )
}

case class ManyRefAttribute[T](
  override val ident: Keyword,
  override val doc: Option[String] = None,
  override val unique: Option[Unique] = None,
  override val index: Option[Boolean] = None,
  override val fulltext: Option[Boolean] = None,
  override val isComponent: Option[Boolean] = None,
  override val noHistory: Option[Boolean] = None
) extends Attribute[DRef, CardinalityMany.type] {

  override val valueType = SchemaType.ref
  override val cardinality = CardinalityMany

  def withDoc(str: String) = copy[T]( doc = Some(str) )
  def withUnique(u: Unique) = copy[T]( unique = Some(u) )
  def withIndex(b: Boolean) = copy[T]( index = Some(b) )
  def withFullText(b: Boolean) = copy[T]( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy[T]( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy[T]( noHistory = Some(b) )
}


sealed trait Props {
  def convert: PartialAddEntity

  private def ::[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): Props = PropsLink(prop, this, attrC)

  def +[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]) = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => if(head._1 == prop._1) (prop :: tail)(attrC) else (head :: step(tail))(ac)
        case PropsNil => prop :: PropsNil
      }
    }

    step(this)
  }

  def -[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card]) = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => if(head._1 == attr) tail else (head :: step(tail))(ac)
        case PropsNil => PropsNil
      }
    }

    step(this)
  }

  def get[DD <: DatomicData, Card <: Cardinality, A](attr: Attribute[DD, Card])
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): Option[A] = {
    def step(cur: Props): Option[A] = {
      cur match {
        case PropsLink(head, tail, ac) => if(head._1 == attr) Some(head._2.asInstanceOf[A]) else step(tail)
        case PropsNil => None
      }
    }
    step(this)
  }

  def ++(other: Props): Props = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => (head :: step(tail))(ac)
        case PropsNil => other
      }
    }

    step(this)
  }
}

object Props {
  def apply() = PropsNil

  def apply[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))  
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): Props = {
      prop :: PropsNil
  }
}

case object PropsNil extends Props {
  def convert: PartialAddEntity = PartialAddEntity.empty
}

case class PropsLink[DD <: DatomicData, Card <: Cardinality, A](
  head: (Attribute[DD, Card], A), 
  tail: Props, 
  attrC: Attribute2PartialAddEntityWriter[DD, Card, A]
) extends Props {
  override def toString = s"""${head._1.ident} -> ${head._2} :: $tail""" 

  def convert: PartialAddEntity = {
    attrC.convert(head._1).write(head._2) ++ tail.convert
  }
}


trait DatomicSchemaFactFacilities extends DatomicTypeWrapper {
  /** add based on Schema attributes 
    */
  def add[DD <: DatomicData, Card <: Cardinality, A](id: DId)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): AddFact = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: DatomicData) = partial.props.head
    AddFact(id, kw, value)
  }

  def add[DD <: DatomicData, Card <: Cardinality, A](id: DLong)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): AddFact = {
    add(DId(id))(prop)(attrC)
  }

  def add[DD <: DatomicData, Card <: Cardinality, A](id: Long)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): AddFact = {
    add(DId(DLong(id)))(prop)(attrC)
  }

  /** retract based on Schema attributes 
    */
  def retract[DD <: DatomicData, Card <: Cardinality, A](id: DId)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): RetractFact = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: DatomicData) = partial.props.head
    RetractFact(id, kw, value)
  }
  def retract[DD <: DatomicData, Card <: Cardinality, A](id: DLong)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): RetractFact = {
    retract(DId(id))(prop)(attrC)
  }

  def retract[DD <: DatomicData, Card <: Cardinality, A](id: Long)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): RetractFact = {
    retract(DId(DLong(id)))(prop)(attrC)
  }
}

trait DatomicSchemaEntityFacilities extends DatomicTypeWrapper {
  /** AddEntity based on Schema attributes 
    */
  def add(id: DId)(props: Props): AddEntity = AddEntity(id, props.convert)

}

trait DatomicSchemaQueryFacilities {

} 

object SchemaFact extends DatomicSchemaFactFacilities

object SchemaEntity extends DatomicSchemaEntityFacilities

