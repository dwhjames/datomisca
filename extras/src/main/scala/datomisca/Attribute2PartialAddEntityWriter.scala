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


@implicitNotFound("There is no writer for type ${Dest} given an attribute with Datomic type ${DD} and cardinality ${Card}")
trait Attribute2PartialAddEntityWriter[DD <: AnyRef, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[Dest]
}

object Attribute2PartialAddEntityWriter {

  implicit def attr2PartialAddEntityWriterOne[DD <: AnyRef, T](implicit conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Cardinality.one.type, T] {
      override def convert(attr: Attribute[DD, Cardinality.one.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> conv.to(t)))
      }
    }

  implicit def attr2PartialAddEntityWriterMany[DD <: AnyRef, C, T](implicit ev: C <:< Traversable[T], conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DD, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Seq.newBuilder[AnyRef]
            for (e <- c) builder += conv.to(e)
            new PartialAddEntity(Map(attr.ident -> datomic.Util.list(builder.result():_*)))
          }
      }
    }

  implicit def refAttr2PartialAddEntityWriterOne[T](implicit ev: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.one.type, T] {
      override def convert(attr: Attribute[DRef.type, Cardinality.one.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDRef(t)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterSingleton[T](implicit ev: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.many.type, T] {
      override def convert(attr: Attribute[DRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDRef(t)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterMany[C, T](implicit ev: C <:< Traversable[T], conv: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Seq.newBuilder[AnyRef]
            for (e <- c) builder += conv.toDRef(e)
            new PartialAddEntity(Map(attr.ident -> datomic.Util.list(builder.result():_*)))
          }
      }
    }

  implicit def refAttr2PartialAddEntityWriterOneIdView[C, T](implicit witness: C <:< IdView[T]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.one.type, C] {
      override def convert(attr: Attribute[DRef.type, Cardinality.one.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) = new PartialAddEntity(Map(attr.ident -> (witness(c).id: java.lang.Long)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterSingletonIdView[C, T](implicit witness: C <:< IdView[T]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) = new PartialAddEntity(Map(attr.ident -> (witness(c).id: java.lang.Long)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterSingletonManyView[C, T](implicit witness: C <:< Traversable[IdView[T]]) =
    new Attribute2PartialAddEntityWriter[DRef.type, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DRef.type, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) =
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
