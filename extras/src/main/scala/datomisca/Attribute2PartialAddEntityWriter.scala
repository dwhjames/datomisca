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


trait Attribute2PartialAddEntityWriter[DD <: DatomicData, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[Dest]
}

object Attribute2PartialAddEntityWriter {

  implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData, T](implicit conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Cardinality.one.type, T] {
      override def convert(attr: Attribute[DD, Cardinality.one.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> conv.to(t)))
      }
    }

  implicit def attr2PartialAddEntityWriterMany[DD <: DatomicData, C, T](implicit ev: C <:< Traversable[T], conv: ToDatomic[DD, T]) =
    new Attribute2PartialAddEntityWriter[DD, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DD, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Iterable.newBuilder[DatomicData]
            for (e <- c) builder += conv.to(e)
            new PartialAddEntity(Map(attr.ident -> DColl(builder.result)))
          }
      }
    }

  implicit def refAttr2PartialAddEntityWriterOne[T](implicit ev: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef, Cardinality.one.type, T] {
      override def convert(attr: Attribute[DRef, Cardinality.one.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDRef(t)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterSingleton[T](implicit ev: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef, Cardinality.many.type, T] {
      override def convert(attr: Attribute[DRef, Cardinality.many.type]) = new PartialAddEntityWriter[T] {
        override def write(t: T) = new PartialAddEntity(Map(attr.ident -> ev.toDRef(t)))
      }
    }

  implicit def refAttr2PartialAddEntityWriterMany[C, T](implicit ev: C <:< Traversable[T], conv: ToDRef[T]) =
    new Attribute2PartialAddEntityWriter[DRef, Cardinality.many.type, C] {
      override def convert(attr: Attribute[DRef, Cardinality.many.type]) = new PartialAddEntityWriter[C] {
        override def write(c: C) =
          if (c.isEmpty)
            PartialAddEntity.empty
          else {
            val builder = Iterable.newBuilder[DatomicData]
            for (e <- c) builder += conv.toDRef(e)
            new PartialAddEntity(Map(attr.ident -> DColl(builder.result)))
          }
      }
    }

}
