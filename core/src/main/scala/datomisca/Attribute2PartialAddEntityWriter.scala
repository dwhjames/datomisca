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
trait Attribute2PartialAddEntityWriter[DD <: AnyRef, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[T]
}

object Attribute2PartialAddEntityWriter {

  implicit def oneValue[DD <: AnyRef, Card <: Cardinality, T](implicit conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Card, T] {
      override def convert(attr: Attribute[DD, Card]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> conv.to(t)))
      }
    }


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


  implicit def oneRef[Card <: Cardinality, T](implicit ev: AsDatomicRef[T]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Card, T] {
      override def convert(attr: Attribute[DatomicRef.type, Card]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDatomicRef(t)))
      }
    }


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


  implicit def oneIdView[Card <: Cardinality, V, T](implicit witness: V <:< IdView[T]) =
    new Attribute2PartialAddEntityWriter[DatomicRef.type, Card, V] {
      override def convert(attr: Attribute[DatomicRef.type, Card]) = new PartialAddEntityWriter[V] {
        override def write(c: V) = new PartialAddEntity(Map(attr.ident -> (witness(c).id: java.lang.Long)))
      }
    }


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
