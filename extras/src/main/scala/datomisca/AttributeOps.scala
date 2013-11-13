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


private[datomisca] class AttributeOps[DD <: AnyRef, Card <: Cardinality](attr: Attribute[DD, Card])
{
  def read[A](implicit a2er: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[A] =
    a2er.convert(attr)

  def readOpt[A](implicit a2er: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[Option[A]] =
    EntityReader[Option[A]] { e: DEntity => 
      // searches attributes in the entity before reading it
      e.get(attr.ident) match {
        case None => None
        case Some(_) => Some(a2er.convert(attr).read(e))
      }
    }

  def write[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[A] = a2ew.convert(attr)
  def writeOpt[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[Option[A]] = 
    PartialAddEntityWriter[Option[A]] { a => a match {
      case None => PartialAddEntity.empty
      case Some(a) => a2ew.convert(attr).write(a)
    } }
}  
