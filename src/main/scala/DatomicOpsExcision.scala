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

case class ExciseEntity(
  id: Long, excisionId: TempId = DId(Partition.USER), attrs: Set[Keyword] = Set(),
  before: Option[Either[java.util.Date, Long]] = None
) extends Operation with FinalIdentified {
  def before(d: java.util.Date) = this.copy(before = Some(Left(d)))
  def before(tx: Long) = this.copy(before = Some(Right(tx)))

  lazy val props = {
    var m: Map[Keyword, DatomicData] = Map(
      Keyword("id", Namespace.DB) -> excisionId,
      Keyword("excise", Namespace.DB) -> FinalId(id)
    )

    if(!attrs.isEmpty) 
      m = m + (Keyword("attrs", Namespace.DB.EXCISE) -> DColl(attrs.map(DRef(_))))

    before.foreach{
      case Left(d: java.util.Date) => m = m + (Keyword("before", Namespace.DB.EXCISE) -> DInstant(d))
      case Right(tx: Long) => m = m + (Keyword("beforeT", Namespace.DB.EXCISE) -> DLong(tx))
    }

    m.map{ case (kw, dd) => (kw.toNative, dd.toNative) }.asJava
  }

  def toNative: AnyRef = {
    props
  }

  override def toString = props.toString
}

case class ExciseAttr(
  attr: Keyword, excisionId: TempId = DId(Partition.USER),
  before: Option[Either[java.util.Date, Long]]
) extends Operation {
  def before(d: java.util.Date) = this.copy(before = Some(Left(d)))
  def before(tx: Long) = this.copy(before = Some(Right(tx)))

  lazy val props =
    before match {
      case None => // BE CAREFUL it excises All Values of An Attribute
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, DRef(attr).toNative
        )
      case Some(Left(d)) =>
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, DRef(attr).toNative,
          Keyword("before", Namespace.DB.EXCISE).toNative, DInstant(d).toNative
        )

      case Some(Right(tx)) =>
        datomic.Util.map(
          Keyword("id", Namespace.DB).toNative, excisionId.toNative,
          Keyword("excise", Namespace.DB).toNative, DRef(attr).toNative,
          Keyword("beforeT", Namespace.DB.EXCISE).toNative, DLong(tx).toNative
        )
    }

  def toNative: AnyRef = {
    props
  }

  override def toString = props.toString
}
