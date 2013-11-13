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


object SchemaFact {
  /** add based on Schema attributes 
    */
  def add[T, DD <: AnyRef, Card <: Cardinality, A](id: T)(prop: (Attribute[DD, Card], A))
    (implicit ev: AsEntityId[T], attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): AddFact = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: AnyRef) = partial.props.head
    AddFact(ev.conv(id), kw, value)
  }

  /** retract based on Schema attributes 
    */
  def retract[T, DD <: AnyRef, Card <: Cardinality, A](id: T)(prop: (Attribute[DD, Card], A))
    (implicit ev: AsPermanentEntityId[T], attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): RetractFact = {
    val entityWriter = attrC.convert(prop._1)
    val partial = entityWriter.write(prop._2)
    val (kw: Keyword, value: AnyRef) = partial.props.head
    RetractFact(ev.conv(id), kw, value)
  }

}
