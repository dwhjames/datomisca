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

object DatomicMapping 
  extends CombinatorImplicits
{
  def fromEntity[A](e: Entity)(implicit er: EntityReader[A]): A = er.read(e)

  def toEntity[T, A](id: T)(a: A)(implicit ev: AsEntityId[T], ew: PartialAddEntityWriter[A]): AddEntity = new AddEntity(ev.conv(id), ew.write(a).props)

  val ID = Attribute( Namespace.DB / "id", SchemaType.long, Cardinality.one)

  val readId    = ID.read[Long]   (Attribute2EntityReaderCast.attr2EntityReaderCastOne)
  val readIdOpt = ID.readOpt[Long](Attribute2EntityReaderCast.attr2EntityReaderCastOne)

}
