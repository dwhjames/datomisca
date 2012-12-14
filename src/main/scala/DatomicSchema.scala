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

case class Attribute[DD <: DatomicData, Card <: Cardinality](
  ident: Keyword,
  valueType: SchemaType[DD],
  cardinality: Card,
  doc: Option[String] = None,
  unique: Option[Unique] = None,
  index: Option[Boolean] = None,
  fulltext: Option[Boolean] = None,
  isComponent: Option[Boolean] = None,
  noHistory: Option[Boolean] = None
) extends Operation with Identified {
  // using partiton :db.part/db
  override lazy val id = DId(Partition.DB)

  def withDoc(str: String) = copy( doc = Some(str) )
  def withUnique(u: Unique) = copy( unique = Some(u) )
  def withIndex(b: Boolean) = copy( index = Some(b) )
  def withFullText(b: Boolean) = copy( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy( isComponent = Some(b) )
  def withNoHistory(b: Boolean) = copy( noHistory = Some(b) )

  lazy val toAddOps: AddToEntity = {
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

    AddToEntity(id, mb.result())
  }
  
  def toNative: java.lang.Object = toAddOps.toNative

  override def toString = s"""
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

sealed trait Props {
  def convert: PartialAddToEntity

  private def ::[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Props = PropsLink(prop, this, attrC)

  def +[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]) = {
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
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Option[A] = {
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
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Props = {
      prop :: PropsNil
  }
}

case object PropsNil extends Props {
  def convert: PartialAddToEntity = PartialAddToEntity.empty
}

case class PropsLink[DD <: DatomicData, Card <: Cardinality, A](
  head: (Attribute[DD, Card], A), 
  tail: Props, 
  attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]
) extends Props {
  override def toString = s"""${head._1.ident} -> ${head._2} :: $tail""" 

  def convert: PartialAddToEntity = {
    attrC.convert(head._1).write(head._2) ++ tail.convert
  }
}



trait DatomicSchemaFacilities {
  /** add based on Schema attributes 
    */
  def add[DD <: DatomicData, Card <: Cardinality, A](id: DId)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Add = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: DatomicData) = partial.props.head
    Add(id, kw, value)
  }
  def add[DD <: DatomicData, Card <: Cardinality, A](id: DLong)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Add = {
    add(DId(id))(prop)(attrC)
  }

  def add[DD <: DatomicData, Card <: Cardinality, A](id: Long)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Add = {
    add(DId(DLong(id)))(prop)(attrC)
  }

  /** retract based on Schema attributes 
    */
  def retract[DD <: DatomicData, Card <: Cardinality, A](id: DId)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Retract = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: DatomicData) = partial.props.head
    Retract(id, kw, value)
  }
  def retract[DD <: DatomicData, Card <: Cardinality, A](id: DLong)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Retract = {
    retract(DId(id))(prop)(attrC)
  }

  def retract[DD <: DatomicData, Card <: Cardinality, A](id: Long)(prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddToEntityWriter[DD, Card, A]): Retract = {
    retract(DId(DLong(id)))(prop)(attrC)
  }

  /** addToEntity based on Schema attributes 
    */
  def addToEntity(id: DId)(props: Props): AddToEntity = {
    AddToEntity(id, props.convert)
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1](id: DId, prop1: (Attribute[DD1, Card1], A1))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1]): AddToEntity = {
    AddToEntity(id, attrC1.convert(prop1._1).write(prop1._2))
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1, 
                  DD2 <: DatomicData, Card2 <: Cardinality, A2]
                  (id: DId,
                   prop1: (Attribute[DD1, Card1], A1), 
                   prop2: (Attribute[DD2, Card2], A2))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1],
              attrC2: Attribute2PartialAddToEntityWriter[DD2, Card2, A2]): AddToEntity = {
    AddToEntity(id, 
      attrC1.convert(prop1._1).write(prop1._2) ++
      attrC2.convert(prop2._1).write(prop2._2) 
    )
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1, 
                  DD2 <: DatomicData, Card2 <: Cardinality, A2,
                  DD3 <: DatomicData, Card3 <: Cardinality, A3]
                  (id: DId,
                   prop1: (Attribute[DD1, Card1], A1), 
                   prop2: (Attribute[DD2, Card2], A2), 
                   prop3: (Attribute[DD3, Card3], A3))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1],
              attrC2: Attribute2PartialAddToEntityWriter[DD2, Card2, A2],
              attrC3: Attribute2PartialAddToEntityWriter[DD3, Card3, A3]): AddToEntity = {
    AddToEntity(id, 
      attrC1.convert(prop1._1).write(prop1._2) ++ 
      attrC2.convert(prop2._1).write(prop2._2) ++ 
      attrC3.convert(prop3._1).write(prop3._2) 
    )
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1, 
                  DD2 <: DatomicData, Card2 <: Cardinality, A2,
                  DD3 <: DatomicData, Card3 <: Cardinality, A3,
                  DD4 <: DatomicData, Card4 <: Cardinality, A4]
                  (id: DId,
                   prop1: (Attribute[DD1, Card1], A1), 
                   prop2: (Attribute[DD2, Card2], A2), 
                   prop3: (Attribute[DD3, Card3], A3), 
                   prop4: (Attribute[DD4, Card4], A4))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1],
              attrC2: Attribute2PartialAddToEntityWriter[DD2, Card2, A2],
              attrC3: Attribute2PartialAddToEntityWriter[DD3, Card3, A3],
              attrC4: Attribute2PartialAddToEntityWriter[DD4, Card4, A4]): AddToEntity = {
    AddToEntity(id, 
      attrC1.convert(prop1._1).write(prop1._2) ++ 
      attrC2.convert(prop2._1).write(prop2._2) ++ 
      attrC3.convert(prop3._1).write(prop3._2) ++ 
      attrC4.convert(prop4._1).write(prop4._2) 
    )
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1, 
                  DD2 <: DatomicData, Card2 <: Cardinality, A2,
                  DD3 <: DatomicData, Card3 <: Cardinality, A3,
                  DD4 <: DatomicData, Card4 <: Cardinality, A4,
                  DD5 <: DatomicData, Card5 <: Cardinality, A5]
                  (id: DId,
                   prop1: (Attribute[DD1, Card1], A1), 
                   prop2: (Attribute[DD2, Card2], A2), 
                   prop3: (Attribute[DD3, Card3], A3), 
                   prop4: (Attribute[DD4, Card4], A4), 
                   prop5: (Attribute[DD5, Card5], A5))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1],
              attrC2: Attribute2PartialAddToEntityWriter[DD2, Card2, A2],
              attrC3: Attribute2PartialAddToEntityWriter[DD3, Card3, A3],
              attrC4: Attribute2PartialAddToEntityWriter[DD4, Card4, A4],
              attrC5: Attribute2PartialAddToEntityWriter[DD5, Card5, A5]): AddToEntity = {
    AddToEntity(id, 
      attrC1.convert(prop1._1).write(prop1._2) ++ 
      attrC2.convert(prop2._1).write(prop2._2) ++ 
      attrC3.convert(prop3._1).write(prop3._2) ++ 
      attrC4.convert(prop4._1).write(prop4._2) ++ 
      attrC5.convert(prop5._1).write(prop5._2) 
    )
  }

  def addToEntity[DD1 <: DatomicData, Card1 <: Cardinality, A1, 
                  DD2 <: DatomicData, Card2 <: Cardinality, A2,
                  DD3 <: DatomicData, Card3 <: Cardinality, A3,
                  DD4 <: DatomicData, Card4 <: Cardinality, A4,
                  DD5 <: DatomicData, Card5 <: Cardinality, A5,
                  DD6 <: DatomicData, Card6 <: Cardinality, A6]
                  (id: DId,
                   prop1: (Attribute[DD1, Card1], A1), 
                   prop2: (Attribute[DD2, Card2], A2), 
                   prop3: (Attribute[DD3, Card3], A3), 
                   prop4: (Attribute[DD4, Card4], A4), 
                   prop5: (Attribute[DD5, Card5], A5), 
                   prop6: (Attribute[DD6, Card6], A6))
    (implicit attrC1: Attribute2PartialAddToEntityWriter[DD1, Card1, A1],
              attrC2: Attribute2PartialAddToEntityWriter[DD2, Card2, A2],
              attrC3: Attribute2PartialAddToEntityWriter[DD3, Card3, A3],
              attrC4: Attribute2PartialAddToEntityWriter[DD4, Card4, A4],
              attrC5: Attribute2PartialAddToEntityWriter[DD5, Card5, A5],
              attrC6: Attribute2PartialAddToEntityWriter[DD6, Card6, A6]): AddToEntity = {
    AddToEntity(id, 
      attrC1.convert(prop1._1).write(prop1._2) ++ 
      attrC2.convert(prop2._1).write(prop2._2) ++ 
      attrC3.convert(prop3._1).write(prop3._2) ++ 
      attrC4.convert(prop4._1).write(prop4._2) ++ 
      attrC5.convert(prop5._1).write(prop5._2) ++ 
      attrC6.convert(prop6._1).write(prop6._2) 
    )
  }
}