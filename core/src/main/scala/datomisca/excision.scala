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

import clojure.lang.Keyword


private[datomisca] class ExciseEntity(
    val id:     Long,
    excisionId: TempId = DId(Partition.USER),
    attrs:      Set[Keyword] = Set(),
    before:     Option[Either[Date, Long]] = None
) extends TxData with FinalIdentified {
  def before(d: Date) = new ExciseEntity(this.id, this.excisionId, this.attrs, Some(Left(d)))
  def before(tx: Long) = new ExciseEntity(this.id, this.excisionId, this.attrs, Some(Right(tx)))

  lazy val props = {
    val builder = Map.newBuilder[AnyRef, AnyRef]

    builder += ((Namespace.DB / "id") -> excisionId.toDatomicId)
    builder += ((Namespace.DB / "excise") -> (id: java.lang.Long))

    if(!attrs.isEmpty)
      builder += ((Namespace.DB.EXCISE / "attrs") -> datomic.Util.list(attrs.toSeq:_*))

    before foreach {
      case Left(d: Date) =>
        builder += ((Namespace.DB.EXCISE / "before") -> d)
      case Right(tx: Long) =>
        builder += ((Namespace.DB.EXCISE / "beforeT") -> (tx: java.lang.Long))
    }

    builder.result().asJava
  }

  def toTxData: AnyRef = {
    props
  }

  override def toString = props.toString
}

private[datomisca] class ExciseAttr(
    attr:       Keyword,
    excisionId: TempId = DId(Partition.USER),
    before:     Option[Either[Date, Long]]
) extends TxData {
  def before(d: Date) = new ExciseAttr(this.attr, this.excisionId, Some(Left(d)))
  def before(tx: Long) = new ExciseAttr(this.attr, this.excisionId, Some(Right(tx)))

  lazy val props =
    before match {
      case None => // BE CAREFUL it excises All Values of An Attribute
        datomic.Util.map(
          (Namespace.DB / "id"), excisionId.toDatomicId,
          (Namespace.DB / "excise"), attr
        )
      case Some(Left(d)) =>
        datomic.Util.map(
          (Namespace.DB / "id"), excisionId.toDatomicId,
          (Namespace.DB / "excise"), attr,
          (Namespace.DB.EXCISE / "before"), d
        )

      case Some(Right(tx)) =>
        datomic.Util.map(
          (Namespace.DB / "id"), excisionId.toDatomicId,
          (Namespace.DB / "excise"), attr,
          (Namespace.DB.EXCISE / "beforeT"), (tx: java.lang.Long)
        )
    }

  def toTxData: AnyRef = {
    props
  }

  override def toString = props.toString
}

object Excise {
  /** Create operations to excise partialy an entity
    * @param id the targeted [[DId]] which must be a Long
    * @param excisionId the temporary ID of the excision entity 
    * @param attr attribute to excised from entity (partial excision)
    */
  def entity[T](id: T, excisionId: TempId, attr: Keyword, attrs: Keyword*)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), excisionId, (attr +: attrs).toSet)
  def entity[T](id: T, attr: Keyword, attrs: Keyword*)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), attrs = (attr +: attrs).toSet)

  /** Create operations to excise a full entity
    * @param id the targeted [[DId]] which must be a Long or [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    */
  def entity[T](id: T, excisionId: TempId)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), excisionId)
  def entity[T](id: T)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id))

  /** Create operations to excise entity restricting excision to datoms created before a tx
    * @param id the targeted [[DId]] which must be a Long
    * @param excisionId the temporary ID of the excision entity
    * @param before the transaction id before which datoms excision is limited
    */
  def entity[T](id: T, excisionId: TempId, before: Long)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), excisionId=excisionId, before=Some(Right(before)))
  def entity[T](id: T, before: Long)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), before=Some(Right(before)))

  /** Create operations to excise entity restricting excision to datoms created before a date
    * @param id the targeted [[DId]] which must be a Long
    * @param excisionId the temporary ID of the excision entity
    * @param before the instant before which datoms excision is limited
    */
  def entity[T](id: T, excisionId: TempId, before: Date)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), excisionId=excisionId, before=Some(Left(before)))
  def entity[T](id: T, before: Date)(implicit ev: AsPermanentEntityId[T]) =
    new ExciseEntity(ev.conv(id), before=Some(Left(before)))


  /** Create operations to excise all attributes restricting to datoms created before a date
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the instant before which datoms excision is limited
    */
  def attribute(attr: Keyword, excisionId: TempId, before: Date) =
    new ExciseAttr(attr=attr, excisionId=excisionId, before=Some(Left(before)))
  def attribute(attr: Keyword, before: Date) =
    new ExciseAttr(attr=attr, before=Some(Left(before)))

  /** Create operations to excise all attributes restricting to datoms created before a transaction
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    * @param before the transaction before which datoms excision is limited
    */
  def attribute(attr: Keyword, excisionId: TempId, before: Long) =
    new ExciseAttr(attr=attr, excisionId=excisionId, before=Some(Right(before)))
  def attribute(attr: Keyword, before: Long) = new ExciseAttr(attr=attr, before=Some(Right(before)))


  /** WARNING: this removes ALL values of this attribute
    * Creates operations to excise all attributes restricting to datoms created before a transaction
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param excisionId the temporary ID of the excision entity
    */
  def attribute(attr: Keyword, excisionId: TempId) = new ExciseAttr(attr=attr, excisionId=excisionId, before=None)
  def attribute(attr: Keyword) = new ExciseAttr(attr=attr, before=None)
}

