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

import scala.language.reflectiveCalls

package object datomisca {

  implicit class RichDEntity(entity: DEntity) {

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
    def apply[DD <: DatomicData, Card <: Cardinality, T]
             (attr: Attribute[DD, Card])
             (implicit attrC: Attribute2EntityReaderInj[DD, Card, T])
             : T =
      attrC.convert(attr).read(entity)

    /**
      * An optional version of apply
      */
    def get[DD <: DatomicData, Card <: Cardinality, T]
           (attr: Attribute[DD, Card])
           (implicit attrC: Attribute2EntityReaderInj[DD, Card, T])
           : Option[T] =
      try {
        Some(apply(attr))
      } catch {
        case ex: EntityKeyNotFoundException => None
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
    def read[T] = new {
      def apply[DD <: DatomicData, Card <: Cardinality]
               (attr: Attribute[DD, Card])
               (implicit attrC: Attribute2EntityReaderCast[DD, Card, T])
               : T =
      attrC.convert(attr).read(entity)
    }

    /**
      * An optional version of read
      */
    def readOpt[T] = new {
      def apply[DD <: DatomicData, Card <: Cardinality]
               (attr: Attribute[DD, Card])
               (implicit attrC: Attribute2EntityReaderCast[DD, Card, T])
               : Option[T] =
      try {
        Some(read[T](attr))
      } catch {
        case ex: EntityKeyNotFoundException => None
      }
    }

    /**
      *
      * @return the IdView of the entity referenced by the given attribute
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def idView[T]
              (attr: Attribute[DRef, Cardinality.one.type])
              (implicit attrC: Attribute2EntityReaderCast[DRef, Cardinality.one.type, IdView[T]])
              : IdView[T] =
      read[IdView[T]](attr)

    /**
      * An optional version of idView
      */
    def getIdView[T]
                 (attr: Attribute[DRef, Cardinality.one.type])
                 (implicit attrC: Attribute2EntityReaderCast[DRef, Cardinality.one.type, IdView[T]])
                 : Option[IdView[T]] =
      readOpt[IdView[T]](attr)

    /**
      *
      * @return the set of IdViews of the entities referenced by the given attribute
      * @throws EntityKeyNotFoundException when the attribute does not exist
      */
    def idViews[T]
               (attr: Attribute[DRef, Cardinality.many.type])
               (implicit attrC: Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[IdView[T]]])
               : Set[IdView[T]] =
      read[Set[IdView[T]]](attr)

    /**
      * An optional version of idViews
      */
    def getIdViews[T]
                  (attr: Attribute[DRef, Cardinality.many.type])
                  (implicit attrC: Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[IdView[T]]])
                  : Option[Set[IdView[T]]] =
      readOpt[Set[IdView[T]]](attr)
  }
}
