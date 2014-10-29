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

package object datomisca {

  type Keyword = clojure.lang.Keyword

  implicit class RichEntity(entity: Entity) {

    /**
      * Get the value of the entity's attribute.
      *
      * The return type is inferred automatically as the implicit
      * ensures there is a unique return type for the Datomic
      * data type specified by the attribute.
      *
      * @return the value of the attribute for this entity
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def apply[DD <: AnyRef, Card <: Cardinality, T]
             (attr: Attribute[DD, Card])
             (implicit attrC: Attribute2EntityReaderInj[DD, Card, T])
             : T =
      attrC.convert(attr).read(entity)

    /**
      * An optional version of apply
      */
    def get[DD <: AnyRef, Card <: Cardinality, T]
           (attr: Attribute[DD, Card])
           (implicit attrC: Attribute2EntityReaderInj[DD, Card, T])
           : Option[T] = {
      if (entity.contains(attr.ident)) {
        Some(attrC.convert(attr).read(entity))
      } else {
        None
      }
    }

    /**
      * Get the value of the entity's attribute.
      *
      * The return type must be explicitly specified, and the
      * implicit ensures that it is a valid pairing with the
      * Datomic data type specified by the attribute.
      *
      * @return the value of the attribute for this entity
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def read[T] = new ReadHelper[T]

    class ReadHelper[T] {
      def apply[DD <: AnyRef, Card <: Cardinality]
               (attr: Attribute[DD, Card])
               (implicit attrC: Attribute2EntityReaderCast[DD, Card, T])
               : T =
        attrC.convert(attr).read(entity)
    }

    /**
      * An optional version of read
      */
    def readOpt[T] = new ReadOptHelper[T]

    class ReadOptHelper[T] {
      def apply[DD <: AnyRef, Card <: Cardinality]
               (attr: Attribute[DD, Card])
               (implicit attrC: Attribute2EntityReaderCast[DD, Card, T])
               : Option[T] = {
        if (entity.contains(attr.ident)) {
          Some(attrC.convert(attr).read(entity))
        } else {
          None
        }
      }
    }

    def readWithDefault[DD <: AnyRef, Card <: Cardinality, T]
               (attr: Attribute[DD, Card], default: => T)
               (implicit attrC: Attribute2EntityReaderCast[DD, Card, T])
               : T = {
      if (entity.contains(attr.ident)) {
        attrC.convert(attr).read(entity)
      } else {
        default
      }
    }

    /**
      *
      * @return the IdView of the entity referenced by the given attribute
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def idView[T]
              (attr: Attribute[DatomicRef.type, Cardinality.one.type])
              (implicit attrC: Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, IdView[T]])
              : IdView[T] =
      attrC.convert(attr).read(entity)

    /**
      * An optional version of idView
      */
    def getIdView[T]
                 (attr: Attribute[DatomicRef.type, Cardinality.one.type])
                 (implicit attrC: Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, IdView[T]])
                 : Option[IdView[T]] = {
      if (entity.contains(attr.ident)) {
        Some(attrC.convert(attr).read(entity))
      } else {
        None
      }
    }

    /**
      *
      * @return the set of IdViews of the entities referenced by the given attribute
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def idViews[T]
               (attr: Attribute[DatomicRef.type, Cardinality.many.type])
               (implicit attrC: Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[IdView[T]]])
               : Set[IdView[T]] =
      attrC.convert(attr).read(entity)

    /**
      * An optional version of idViews
      */
    def getIdViews[T]
                  (attr: Attribute[DatomicRef.type, Cardinality.many.type])
                  (implicit attrC: Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[IdView[T]]])
                  : Option[Set[IdView[T]]] = {
      if (entity.contains(attr.ident)) {
        Some(attrC.convert(attr).read(entity))
      } else {
        None
      }
    }

  }


  implicit class RichAttribute[DD <: AnyRef, Card <: Cardinality](val attribute: Attribute[DD, Card]) extends AnyVal {

    def read[A](implicit ev: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[A] =
      ev.convert(attribute)

    def readOpt[A](implicit ev: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[Option[A]] =
      EntityReader[Option[A]] { e: Entity =>
        if (e.contains(attribute.ident))
          Some(ev.convert(attribute).read(e))
        else
          None
      }

    def readWithDefault[A](default: => A)(implicit ev: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[A] =
      EntityReader[A] { e: Entity =>
        if (e.contains(attribute.ident))
          ev.convert(attribute).read(e)
        else
          default
      }

    def write[A](implicit ev: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[A] =
      ev.convert(attribute)

    def writeOpt[A](implicit ev: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[Option[A]] = 
      PartialAddEntityWriter[Option[A]] {
        case None    => PartialAddEntity.empty
        case Some(a) => ev.convert(attribute).write(a)
      }

  }

}
