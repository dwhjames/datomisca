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


final case class Attribute[DD, Card <: Cardinality](
    override val ident: Keyword,
    valueType:   SchemaType[DD],
    cardinality: Card,
    doc:         Option[String]  = None,
    unique:      Option[Unique]  = None,
    index:       Option[Boolean] = None,
    fulltext:    Option[Boolean] = None,
    isComponent: Option[Boolean] = None,
    noHistory:   Option[Boolean] = None
) extends TxData with KeywordIdentified {

  def withDoc(str: String)        = copy( doc = Some(str) )
  def withUnique(u: Unique)       = copy( unique = Some(u) )
  def withIndex(b: Boolean)       = copy( index = Some(b) )
  def withFullText(b: Boolean)    = copy( fulltext = Some(b) )
  def withIsComponent(b: Boolean) = copy( isComponent = Some(b) )
  def withNoHistory(b: Boolean)   = copy( noHistory = Some(b) )

  def reverse(implicit ev: =:=[DatomicRef.type, DD]): Attribute[DatomicRef.type, Cardinality.many.type] =
    copy(
      ident       = clojure.lang.Keyword.intern(ident.getNamespace, "_" + ident.getName),
      valueType   = SchemaType.ref,
      cardinality = Cardinality.many
    )

  // using partiton :db.part/db
  val id = DId(Partition.DB)

  lazy val toAddOps: AddEntity = {
    val mb = new scala.collection.mutable.MapBuilder[Keyword, AnyRef, Map[Keyword, AnyRef]](Map(
      Attribute.id          -> id,
      Attribute.ident       -> ident,
      Attribute.valueType   -> valueType.keyword,
      Attribute.cardinality -> cardinality.keyword
    ))
    if(doc.isDefined) mb += Attribute.doc -> doc.get
    if(unique.isDefined) mb += Attribute.unique -> unique.get.keyword
    if(index.isDefined) mb += Attribute.index -> (index.get: java.lang.Boolean)
    if(fulltext.isDefined) mb += Attribute.fulltext -> (fulltext.get: java.lang.Boolean)
    if(isComponent.isDefined) mb += Attribute.isComponent -> (isComponent.get: java.lang.Boolean)
    if(noHistory.isDefined) mb += Attribute.noHistory -> (noHistory.get: java.lang.Boolean)
    
    // installing attribute
    mb += Attribute.installAttr -> Partition.DB.keyword

    new AddEntity(id, mb.result())
  }
  
  override def toTxData: AnyRef = toAddOps.toTxData
  override def toString = ident.toString

  def toEDNString: String = {
    val builder = new StringBuilder(100)
    (builder append '{'
        append Attribute.id          append ' ' append id                append ", "
        append Attribute.ident       append ' ' append ident             append ", "
        append Attribute.valueType   append ' ' append valueType.keyword append ", "
        append Attribute.cardinality append ' ' append cardinality.keyword)
    if (doc.isDefined) (
      builder append ", " append Attribute.doc append ' ' append doc.get
    )
    if (unique.isDefined) (
      builder append ", " append Attribute.unique append ' ' append unique.get
    )
    if (index.isDefined) (
      builder append ", " append Attribute.index append ' ' append index.get
    )
    if (fulltext.isDefined) (
      builder append ", " append Attribute.fulltext append ' ' append fulltext.get
    )
    if (isComponent.isDefined) (
      builder append ", " append Attribute.isComponent append ' ' append isComponent.get
    )
    if (noHistory.isDefined) (
      builder append ", " append Attribute.noHistory append ' ' append noHistory.get
    )
    builder append '}'
    builder.result()
  }

} 

object Attribute {
  val id          = Namespace.DB / "id"
  val ident       = Namespace.DB / "ident"
  val valueType   = Namespace.DB / "valueType"
  val cardinality = Namespace.DB / "cardinality"
  val doc         = Namespace.DB / "doc"
  val unique      = Namespace.DB / "unique"
  val index       = Namespace.DB / "index"
  val fulltext    = Namespace.DB / "fulltext"
  val isComponent = Namespace.DB / "isComponent"
  val noHistory   = Namespace.DB / "noHistory"
  val installAttr = Namespace.DB.INSTALL / "_attribute"
}
