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


trait Attribute2EntityReaderInj[DD <: DatomicData, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderInj {

  /*
   * The values of reference attributes may be other entities,
   * or they may be idents. We have to be conservative and
   * return DatomicData so that the user can determine the
   * precise type.
   */
  implicit val attr2EntityReaderDRef2DD =
    new Attribute2EntityReaderInj[DRef, Cardinality.one.type, DatomicData] {
      def convert(attr: Attribute[DRef, Cardinality.one.type]): EntityReader[DatomicData] =
        EntityReader { entity => entity(attr.ident) }
    }
  // similarly for multi-valued attributes
  implicit val attr2EntityReaderManyDRef2DD =
    new Attribute2EntityReaderInj[DRef, Cardinality.many.type, Set[DatomicData]] {
      def convert(attr: Attribute[DRef, Cardinality.many.type]): EntityReader[Set[DatomicData]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) => elems.toSet } getOrElse (Set.empty)
        }
    }

  /*
   * the given attribute determines the subtype of DatomicData
   * and from that subtype, FromDatomicInj uniquely determines
   * the result type A
   */
  implicit def attr2EntityReaderOne[DD <: DatomicData, A](implicit fdat: FromDatomicInj[DD, A]) = 
    new Attribute2EntityReaderInj[DD, Cardinality.one.type, A] {
      def convert(attr: Attribute[DD, Cardinality.one.type]): EntityReader[A] =
        EntityReader { entity =>
          val dd = entity(attr.ident).asInstanceOf[DD]
          fdat.from(dd)
        }
    }  
  // similarly for multi-valued attributes
  implicit def attr2EntityReaderMany[DD <: DatomicData, A](implicit fdat: FromDatomicInj[DD, A]) = 
    new Attribute2EntityReaderInj[DD, Cardinality.many.type, Set[A]] {
      def convert(attr: Attribute[DD, Cardinality.many.type]): EntityReader[Set[A]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) =>
            elems.map { elem => fdat.from(elem.asInstanceOf[DD]) } .toSet
          } getOrElse (Set.empty)
        }
    }

}



trait Attribute2EntityReaderCast[DD <: DatomicData, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderCast {

  implicit def attr2EntityReaderCastOne[DD <: DatomicData, A](implicit fdat: FromDatomic[DD, A]) =
      new Attribute2EntityReaderCast[DD, Cardinality.one.type, A] {
        def convert(attr: Attribute[DD, Cardinality.one.type]): EntityReader[A] =
          EntityReader { entity =>
            val dd = entity(attr.ident).asInstanceOf[DD]
            fdat.from(dd)
          }
      }

  implicit def attr2EntityReaderCastMany[DD <: DatomicData, A](implicit fdat: FromDatomic[DD, A]) =
  new Attribute2EntityReaderCast[DD, Cardinality.many.type, Set[A]] {
    def convert(attr: Attribute[DD, Cardinality.many.type]): EntityReader[Set[A]] =
      EntityReader { entity =>
        entity.get(attr.ident) map { case DColl(elems) =>
          elems.map { elem => fdat.from(elem.asInstanceOf[DD]) } .toSet
        } getOrElse (Set.empty)
      }
  }


  implicit val attr2EntityReaderCastIdOnly =
    new Attribute2EntityReaderCast[DRef, Cardinality.one.type, Long] {
      def convert(attr: Attribute[DRef, Cardinality.one.type]): EntityReader[Long] =
        EntityReader { entity =>
          entity(attr.ident).asInstanceOf[DEntity].id
        }
    }  

  implicit val attr2EntityReaderCastManyIdOnly =
    new Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[Long]] {
      def convert(attr: Attribute[DRef, Cardinality.many.type]): EntityReader[Set[Long]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) =>
            elems.map {
              case subent: DEntity => subent.id
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            } .toSet
          } getOrElse (Set.empty)
        }
    }

  implicit val attr2EntityReaderCastKeyword =
    new Attribute2EntityReaderCast[DRef, Cardinality.one.type, Keyword] {
      def convert(attr: Attribute[DRef, Cardinality.one.type]): EntityReader[Keyword] =
        EntityReader { entity =>
          entity(attr.ident).asInstanceOf[DKeyword].underlying
        }
    }

  implicit val attr2EntityReaderCastManyKeyword =
    new Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[Keyword]] {
      def convert(attr: Attribute[DRef, Cardinality.many.type]): EntityReader[Set[Keyword]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) =>
            elems.map {
              case DKeyword(keyword) => keyword
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            } .toSet
          } getOrElse (Set.empty)
        }
    }

  /*
   * we need to have an entity reader for type A in scope
   * we can read the ref value of an attribute as an entity
   * and then use the entity reader to interpet it
   */
  implicit def attr2EntityReaderOneObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, Cardinality.one.type, A] {
      def convert(attr: Attribute[DRef, Cardinality.one.type]): EntityReader[A] =
        EntityReader { entity =>
          val subent = entity(attr.ident).asInstanceOf[DEntity]
          er.read(subent)
        }
    }
  // similarly for multi-valued attributes
  implicit def attr2EntityReaderManyObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[A]] {
      def convert(attr: Attribute[DRef, Cardinality.many.type]): EntityReader[Set[A]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) =>
            elems.map {
              case subent: DEntity => er.read(subent)
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            } .toSet
          } getOrElse (Set.empty)
        }
    }

  /*
   * we need to have an entity reader for type A in scope
   * we can read the ref value of an attribute as an entity
   * and then use the entity reader to interpet it. we
   * return the result of the entity reader along with the
   * id of the transformed entity in an IdView
   */
  implicit def attr2EntityReaderOneIdView[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, Cardinality.one.type, IdView[A]] {
      def convert(attr: Attribute[DRef, Cardinality.one.type]): EntityReader[IdView[A]] =
        EntityReader { entity =>
          val subent = entity(attr.ident).asInstanceOf[DEntity]
          IdView(subent.id)(er.read(subent))
        }
    }  
  // similarly for multi-valued attributes
  implicit def attr2EntityReaderManyIdView[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, Cardinality.many.type, Set[IdView[A]]] {
      def convert(attr: Attribute[DRef, Cardinality.many.type]): EntityReader[Set[IdView[A]]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DColl(elems) =>
            elems.map {
              case subent: DEntity => IdView(subent.id)(er.read(subent))
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            } .toSet
          } getOrElse (Set.empty)
        }
    }

}
