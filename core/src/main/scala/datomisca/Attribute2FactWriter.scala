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

import scala.annotation.implicitNotFound

/** A type class to convert an [[Attribute]] and a Scala value into
  * a pair of a [[Keyword]] and a Datomic value.
  *
  * @tparam DD
  *     the Datomic value type of the attribute (see [[SchemaType]]).
  * @tparam Card
  *     the cardinality of the attribute (see [[Cardinality]]).
  * @tparam T
  *     the Scala type that the [[EntityReader]] will read.
  */
@implicitNotFound("There is no writer for type ${T} given an attribute with Datomic type ${DD} and cardinality ${Card}")
trait Attribute2FactWriter[DD <: AnyRef, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card], t: T): (Keyword, AnyRef)
  def convert(attrVal: (Attribute[DD, Card], T)): (Keyword, AnyRef) = convert(attrVal._1, attrVal._2)
}

object Attribute2FactWriter {

  /** If there is a conversion to `DD` from `T`
    * (see [[ToDatomic]]) then we can write a value of `T`
    * for an attribute with value type `DD` as
    * its corresponding Datomic type `DD`.
    */
  implicit def oneValue[DD <: AnyRef, Card <: Cardinality, T](implicit ev: ToDatomic[DD, T]) =
    new Attribute2FactWriter[DD, Card, T] {
      override def convert(attr: Attribute[DD, Card], t: T) =
        (attr.ident -> ev.to(t))
    }

  /** If a value of type `T` can be used as a reference value
    * (see [[AsDatomicRef]]) then we can write a value of `T`
    * as an entity id reference for a reference attribute.
    */
  implicit def oneRef[Card <: Cardinality, T](implicit ev: AsDatomicRef[T]) =
    new Attribute2FactWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card], t: T) =
        (attr.ident -> ev.toDatomicRef(t))
    }


  /** If a value of type `T` is a subtype of [[IdView]]
    * then we can write a value of `T`
    * as an entity id reference for a reference attribute.
    */
  implicit def oneIdView[Card <: Cardinality, T, U](implicit ev: T <:< IdView[U]) =
    new Attribute2FactWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card], t: T) =
        (attr.ident -> (ev(t).id: java.lang.Long))
    }

}
