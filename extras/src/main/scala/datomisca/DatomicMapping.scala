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

import functional.CombinatorImplicits

import scala.language.implicitConversions

object DatomicMapping 
  extends CombinatorImplicits
{
  def fromEntity[A](e: DEntity)(implicit er: EntityReader[A]): A = er.read(e)

  def toEntity[T, A](id: T)(a: A)(implicit ev: AsEntityId[T], ew: PartialAddEntityWriter[A]): AddEntity = new AddEntity(ev.conv(id), ew.write(a).props)

  val ID = Attribute( Namespace.DB / "id", SchemaType.long, Cardinality.one)

  val readId    = new AttributeOps(ID).read[Long]   (Attribute2EntityReaderCast.attr2EntityReaderCastOne)
  val readIdOpt = new AttributeOps(ID).readOpt[Long](Attribute2EntityReaderCast.attr2EntityReaderCastOne)

  /*val writeId = new AttributeOps(ID).write[Long](Attribute2PartialAddEntityWriter.attr2PartialAddEntityWriterOne)

  def writeIdOpt(partition: Partition = Partition.USER) = 
    PartialAddEntityWriter[Option[Long]] { a => a match {
      case None => Attribute2PartialAddEntityWriter.attr2PartialAddEntityWriterOne.convert(ID).to(DId(partition))
      case Some(a) => Attribute2PartialAddEntityWriter.attr2PartialAddEntityWriterOne.convert(ID).to(a)
    } }*/

  implicit def attributeOps[DD <: AnyRef, C <: Cardinality](attr: Attribute[DD, C]) = new AttributeOps(attr)

  // implicit def DRef2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
  //   ToDatomic[java.lang.Long, C]{ (ref: C) => witness(ref).id: java.lang.Long }
}
