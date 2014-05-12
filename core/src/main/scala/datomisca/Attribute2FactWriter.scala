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


@implicitNotFound("There is no writer for type ${T} given an attribute with Datomic type ${DD} and cardinality ${Card}")
trait Attribute2FactWriter[DD <: AnyRef, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card], t: T): (Keyword, AnyRef)
  def convert(attrVal: (Attribute[DD, Card], T)): (Keyword, AnyRef) = convert(attrVal._1, attrVal._2)
}

object Attribute2FactWriter {

  implicit def oneValue[DD <: AnyRef, Card <: Cardinality, T](implicit ev: ToDatomic[DD, T]) =
    new Attribute2FactWriter[DD, Card, T] {
      override def convert(attr: Attribute[DD, Card], t: T) =
        (attr.ident -> ev.to(t))
    }


  implicit def oneRef[Card <: Cardinality, T](implicit ev: AsDatomicRef[T]) =
    new Attribute2FactWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card], t: T) =
        (attr.ident -> ev.toDatomicRef(t))
    }


  implicit def oneIdView[Card <: Cardinality, T, U](implicit ev: T <:< IdView[U]) =
    new Attribute2FactWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card], t: T) =
        (attr.ident -> (ev(t).id: java.lang.Long))
    }

}
