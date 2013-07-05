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

import java.util.Date


class ExciseEntity(
    val id:     Long,
    excisionId: TempId                               = DId(Partition.USER),
    attrs:      Set[Keyword]                         = Set(),
    before:     Option[Either[Date, Long]] = None
) extends Operation with FinalIdentified {
  def before(d: Date) = new ExciseEntity(this.id, this.excisionId, this.attrs, Some(Left(d)))
  def before(tx: Long) = new ExciseEntity(this.id, this.excisionId, this.attrs, Some(Right(tx)))

  lazy val props = {
    var m: Map[Keyword, DatomicData] = Map(
      Keyword("id", Namespace.DB) -> excisionId,
      Keyword("excise", Namespace.DB) -> FinalId(id)
    )

    if(!attrs.isEmpty) 
      m = m + (Keyword("attrs", Namespace.DB.EXCISE) -> DColl(attrs.map(DKeyword.apply)))

    before.foreach{
      case Left(d: Date) => m = m + (Keyword("before", Namespace.DB.EXCISE) -> DInstant(d))
      case Right(tx: Long) => m = m + (Keyword("beforeT", Namespace.DB.EXCISE) -> DLong(tx))
    }

    m.map{ case (kw, dd) => (kw.toNative, dd.toNative) }.asJava
  }

  def toNative: AnyRef = {
    props
  }

  override def toString = props.toString
}

class ExciseAttr(
    attr:       Keyword,
    excisionId: TempId = DId(Partition.USER),
    before:     Option[Either[Date, Long]]
) extends Operation {
  def before(d: Date) = new ExciseAttr(this.attr, this.excisionId, Some(Left(d)))
  def before(tx: Long) = new ExciseAttr(this.attr, this.excisionId, Some(Right(tx)))

  lazy val props =
    before match {
      case None => // BE CAREFUL it excises All Values of An Attribute
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, attr.toNative
        )
      case Some(Left(d)) =>
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, attr.toNative,
          Keyword("before", Namespace.DB.EXCISE).toNative, DInstant(d).toNative
        )

      case Some(Right(tx)) =>
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, attr.toNative,
          Keyword("beforeT", Namespace.DB.EXCISE).toNative, DLong(tx).toNative
        )
    }

  def toNative: AnyRef = {
    props
  }

  override def toString = props.toString
}

object Excise {
  /** Create operations to excise partialy an entity
    * @param id the targeted [[DId]] which must be a [[Long]]
    * @param excisionId the temporary ID of the excision entity 
    * @param attr attribute to excised from entity (partial excision)
    */
  def entity(id: Long, excisionId: TempId, attrs: Keyword*) = new ExciseEntity(id, excisionId, attrs.toSet)
  def entity(id: Long, attrs: Keyword*) = new ExciseEntity(id = id, attrs = attrs.toSet)

  /** Create operations to excise a full entity
    * @param id the targeted [[DId]] which must be a [[Long]] or [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    */
  def entity(id: Long, excisionId: TempId) = new ExciseEntity(id, excisionId)
  def entity(id: Long) = new ExciseEntity(id)

  /** Create operations to excise entity restricting excision to datoms created before a tx
    * @param id the targeted [[DId]] which must be a [[Long]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the transaction id before which datoms excision is limited
    */
  def entity(id: Long, excisionId: TempId, before: Long) = new ExciseEntity(id=id, excisionId=excisionId, before=Some(Right(before)))
  def entity(id: Long, before: Long) = new ExciseEntity(id=id, before=Some(Right(before)))

  /** Create operations to excise entity restricting excision to datoms created before a date
    * @param id the targeted [[DId]] which must be a [[Long]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the instant before which datoms excision is limited
    */
  def entity(id: Long, excisionId: TempId, before: Date) = new ExciseEntity(id=id, excisionId=excisionId, before=Some(Left(before)))
  def entity(id: Long, before: Date) = new ExciseEntity(id=id, before=Some(Left(before)))

  /** Create operations to excise partialy an entity
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param attr attribute to excised from entity (partial excision)
    */
  def entity(id: FinalId, excisionId: TempId, attrs: Keyword*) = new ExciseEntity(id.underlying, excisionId, attrs.toSet)
  def entity(id: FinalId, attrs: Keyword*) = new ExciseEntity(id=id.underlying, attrs=attrs.toSet)

  /** Create operations to excise a full entity
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    */
  def entity(id: FinalId, excisionId: TempId) = new ExciseEntity(id.underlying, excisionId)
  def entity(id: FinalId) = new ExciseEntity(id=id.underlying)

  /** Create operations to excise entity restricting excision to datoms created before a transaction
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the transaction before which datoms excision is limited
    */
  def entity(id: FinalId, excisionId: TempId, before: Long) = new ExciseEntity(id=id.underlying, excisionId=excisionId, before=Some(Right(before)))
  def entity(id: FinalId, before: Long) = new ExciseEntity(id=id.underlying, before=Some(Right(before)))

  /** Create operations to excise entity restricting excision to datoms created before a date
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the instant before which datoms excision is limited
    */
  def entity(id: FinalId, excisionId: TempId, before: Date) = new ExciseEntity(id=id.underlying, excisionId=excisionId, before=Some(Left(before)))
  def entity(id: FinalId, before: Date) = new ExciseEntity(id=id.underlying, before=Some(Left(before)))

  /** Create operations to excise all attributes restricting to datoms created before a date
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the instant before which datoms excision is limited
    */
  def attribute(attr: Keyword, excisionId: TempId, before: Date) = new ExciseAttr(attr=attr, excisionId=excisionId, before=Some(Left(before)))
  def attribute(attr: Keyword, before: Date) = new ExciseAttr(attr=attr, before=Some(Left(before)))

  /** Create operations to excise all attributes restricting to datoms created before a transaction
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the transaction before which datoms excision is limited
    */
  def attribute(attr: Keyword, excisionId: TempId, before: Long) = new ExciseAttr(attr=attr, excisionId=excisionId, before=Some(Right(before)))
  def attribute(attr: Keyword, before: Long) = new ExciseAttr(attr=attr, before=Some(Right(before)))


  /** WARNING: this removes ALL values of this attribute
    * Creates operations to excise all attributes restricting to datoms created before a transaction
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    */
  def attribute(attr: Keyword, excisionId: TempId) = new ExciseAttr(attr=attr, excisionId=excisionId, before=None)
  def attribute(attr: Keyword) = new ExciseAttr(attr=attr, before=None)
}

