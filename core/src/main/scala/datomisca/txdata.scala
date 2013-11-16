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

import clojure.lang.Keyword


trait TxData {
  def toTxData: AnyRef
}

final class AddFact(val id: DId, attr: Keyword, value: AnyRef) extends TxData with TempIdentified {
  override def toTxData: AnyRef =
    datomic.Util.list(Namespace.DB / "add", id.toDatomicId, attr, value)
  override def toString = toTxData.toString
}

final class RetractFact(val id: Long, attr: Keyword, value: AnyRef) extends TxData with FinalIdentified {
  override def toTxData: AnyRef =
    datomic.Util.list(Namespace.DB / "retract", id: java.lang.Long, attr, value)
  override def toString = toTxData.toString
}

final class RetractEntity(val id: Long) extends TxData with FinalIdentified {
  def toTxData: AnyRef =
    datomic.Util.list(Namespace.DB.FN / "retractEntity", id: java.lang.Long)
  override def toString = toTxData.toString
}

class PartialAddEntity(val props: Map[Keyword, AnyRef]) {

  def ++(other: PartialAddEntity) = new PartialAddEntity(props ++ other.props)

  def toMap = props
  override def toString = props.toString
}

object PartialAddEntity {

  def empty: PartialAddEntity = new PartialAddEntity(Map())
}

final class AddEntity(val id: DId, partialProps: Map[Keyword, AnyRef]) extends PartialAddEntity(partialProps + (Namespace.DB / "id" -> id.toDatomicId)) with TxData with TempIdentified {

  def toTxData: AnyRef = {
    import scala.collection.JavaConverters._
    props.asJava
  }

  override def toString = toTxData.toString
}

final case class AddIdent(ident: Keyword, partition: Partition = Partition.USER) extends TxData with KeywordIdentified {
  def toTxData = new AddFact(DId(partition), Namespace.DB / "ident", ident).toTxData

  override def toString = toTxData.toString
}


