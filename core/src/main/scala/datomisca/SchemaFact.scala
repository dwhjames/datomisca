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
  def add[T, DD <: AnyRef, Card <: Cardinality, A](id: T)(attrVal: (Attribute[DD, Card], A))
    (implicit ev1: AsEntityId[T], ev2: Attribute2FactWriter[DD, Card, A]): AddFact = {
    val (kw: Keyword, value: AnyRef) = ev2.convert(attrVal)
    new AddFact(ev1.conv(id), kw, value)
  }

  /** retract based on Schema attributes 
    */
  def retract[T, DD <: AnyRef, Card <: Cardinality, A](id: T)(attrVal: (Attribute[DD, Card], A))
    (implicit ev1: AsPermanentEntityId[T], ev2: Attribute2FactWriter[DD, Card, A]): RetractFact = {
    val (kw: Keyword, value: AnyRef) = ev2.convert(attrVal)
    new RetractFact(ev1.conv(id), kw, value)
  }

}
