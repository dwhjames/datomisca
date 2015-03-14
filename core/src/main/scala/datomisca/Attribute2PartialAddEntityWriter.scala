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


/** A type class to convert an [[Attribute]] into a [[PartialAddEntityWriter]].
  *
  * This type class will determine if it is safe to use type `T` as a valid value
  * for an attribute with value type `DD` and cardinality `Card`.
  *
  * @tparam DD
  *     the Datomic value type of the attribute (see [[SchemaType]]).
  * @tparam Card
  *     the cardinality of the attribute (see [[Cardinality]]).
  * @tparam T
  *     the Scala type that the [[PartialAddEntityWriter]] will write.
  */
@implicitNotFound("There is no writer for type ${T} given an attribute with Datomic type ${DD} and cardinality ${Card}")
trait Attribute2PartialAddEntityWriter[DD <: AnyRef, Card <: Cardinality, T] {

  /** Convert an [[Attribute]] into a [[PartialAddEntityWriter]].
    *
    * @param attr
    *     the attribute with value type `DD` and cardinality `Card` to convert.
    * @return a entity writer that will construct an assertion for attribute `attr`
    *     given a value to write.
    */
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[T]
}

object Attribute2PartialAddEntityWriter {

  /** If there is a conversion to `DD` from `T`
    * (see [[ToDatomic]]) then we can write a value of `T`
    * for an attribute with value type `DD` as
    * its corresponding Datomic type `DD`.
    */
  implicit def oneValue[DD <: AnyRef, Card <: Cardinality, T](implicit conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Card, T] {
      override def convert(attr: Attribute[DD, Card]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> conv.to(t)))
      }
    }


  /** If there is a conversion to `DD` from `T`
    * (see [[ToDatomic]]) and `Coll` is a traversable of `T`,
    * then we can write a value of type `Coll`
    * for a cardinality many attribute with value type `DD` as
    * a list of its corresponding Datomic type `DD`.
    */
  implicit def manyValues[DD <: AnyRef, Coll, T](implicit ev: Coll <:< Traversable[T], conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Cardinality.many.type, Coll] {
      override def convert(attr: Attribute[DD, Cardinality.many.type]) = new PartialAddEntityWriter[Coll] {
        override def write(c: Coll) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Seq.newBuilder[AnyRef]
            for (e <- c) builder += conv.to(e)
            new PartialAddEntity(Map(attr.ident -> datomic.Util.list(builder.result():_*)))
          }
      }
    }


  /** If a value of type `T` can be used as a reference value
    * (see [[AsDatomicRef]]) then we can write a value of `T`
    * as an entity id reference for a reference attribute.
    */
  implicit def oneRef[Card <: Cardinality, T](implicit ev: AsDatomicRef[T]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDatomicRef(t)))
      }
    }


  /** If a value of type `T` can be used as a reference value
    * (see [[AsDatomicRef]]) and `Coll` is a traversable of `T`,
    * then we can write a value of type `Coll`
    * as a list of entity ids for a cardinality many reference attribute.
    */
  implicit def manyRefs[Coll, T](implicit ev: Coll <:< Traversable[T], conv: AsDatomicRef[T]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Cardinality.many.type, Coll] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[Coll] {
        override def write(c: Coll) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Seq.newBuilder[AnyRef]
            for (e <- c) builder += conv.toDatomicRef(e)
            new PartialAddEntity(Map(attr.ident -> datomic.Util.list(builder.result():_*)))
          }
      }
    }


  /** If a value of type `T` is a subtype of [[IdView]]
    * then we can write a value of `T`
    * as an entity id reference for a reference attribute.
    */
  implicit def oneIdView[Card <: Cardinality, V, T](implicit witness: V <:< IdView[T]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Card, V] {
      override def convert(attr: Attribute[DatomicRef.type, Card]) = new PartialAddEntityWriter[V] {
        override def write(c: V) = new PartialAddEntity(Map(attr.ident -> (witness(c).id: java.lang.Long)))
      }
    }


  /** If a value of type `T` is a subtype of [[IdView]]
    * and `Coll` is a traversable of `IdView[T]`,
    * then we can write a value of type `Coll`
    * as a list of entity ids for a cardinality many reference attribute.
    */
  implicit def manyIdViews[Coll, T](implicit witness: Coll <:< Traversable[IdView[T]]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Cardinality.many.type, Coll] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[Coll] {
        override def write(c: Coll) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Seq.newBuilder[AnyRef]
            for (e <- witness(c)) builder += (e.id: java.lang.Long)
            new PartialAddEntity(Map(attr.ident -> datomic.Util.list(builder.result():_*)))
          }
      }
    }

}
