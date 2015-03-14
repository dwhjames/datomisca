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


/** A type class to convert an [[Attribute]] into an [[EntityReader]].
  *
  * This type class will determine a unique type `T` given a Datomic
  * type `DD` and cardinality `Card`.
  *
  * @tparam DD
  *     the Datomic value type of the attribute (see [[SchemaType]]).
  * @tparam Card
  *     the cardinality of the attribute (see [[Cardinality]]).
  * @tparam T
  *     the Scala type that the [[EntityReader]] will read.
  */
@implicitNotFound("There is no unique reader for type ${T} given an attribute with Datomic type ${DD} and cardinality ${Card} to type ${T}")
sealed trait Attribute2EntityReaderInj[DD <: AnyRef, Card <: Cardinality, T] {

  /** Convert an [[Attribute]] into an [[EntityReader]]
    *
    * @param attr
    *     the attribute with value type `DD` and cardinality `Card` to convert.
    * @return an entity reader that will read the value for attribute `attr`
    *     given an entity with that attribute.
    */
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderInj {

  /** A cardinality one reference attribute could return either an
    * [[Entity]] or a [[Keyword]], therefore we can only say that
    * the return type is `Any`.
    */
  implicit val attr2EntityReaderDRef2DD =
    new Attribute2EntityReaderInj[DatomicRef.type, Cardinality.one.type, Any] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.one.type]) = new EntityReader[Any] {
        override def read(entity: Entity) = entity(attr.ident)
      }
    }


  /** A cardinality many reference attribute could return a set of
    * [[Entity]]s or [[Keyword]]s, therefore we can only say that
    * the return type is a `Set` of `Any`.
    */
  implicit val attr2EntityReaderManyDRef2DD =
    new Attribute2EntityReaderInj[DatomicRef.type, Cardinality.many.type, Set[Any]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new EntityReader[Set[Any]] {
        override def read(entity: Entity) =
          entity.get(attr.ident) map { case c: Iterable[Any] => c.toSet } getOrElse (Set.empty)
      }
    }


  /** If there is a unique conversion for `DD` to `A`,
    * then we can read an `A`
    * for an attribute with value type `DD`.
    */
  implicit def attr2EntityReaderOne[DD <: AnyRef, A](implicit conv: FromDatomicInj[DD, A]) =
    new Attribute2EntityReaderInj[DD, Cardinality.one.type, A] {
      override def convert(attr: Attribute[DD, Cardinality.one.type]) = new EntityReader[A] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            conv.from(o.asInstanceOf[DD])
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** If there is a unique conversion for `DD` to `A`,
    * then we can read a `Set` of `A`
    * for a many attribute with value type `DD`.
    */
  implicit def attr2EntityReaderMany[DD <: AnyRef, A](implicit conv: FromDatomicInj[DD, A]) =
    new Attribute2EntityReaderInj[DD, Cardinality.many.type, Set[A]] {
      override def convert(attr: Attribute[DD, Cardinality.many.type]) = new EntityReader[Set[A]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[A]
                val iter = coll.iterator
                while (iter.hasNext) {
                  builder += conv.from(iter.next().asInstanceOf[DD])
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[A]
        }
      }
    }

}



/** A type class to convert an [[Attribute]] into an [[EntityReader]].
  *
  * This type class will determine if it is safe to cast to type `T`
  * given a Datomic type `DD` and cardinality `Card`.
  *
  * @tparam DD
  *     the Datomic value type of the attribute (see [[SchemaType]]).
  * @tparam Card
  *     the cardinality of the attribute (see [[Cardinality]]).
  * @tparam T
  *     the Scala type that the [[EntityReader]] will read.
  */
@implicitNotFound("There is no type-casting reader for type ${T} given an attribute with Datomic type ${DD} and cardinality ${Card} to type ${T}")
trait Attribute2EntityReaderCast[DD <: AnyRef, Card <: Cardinality, T] {

  /** Convert an [[Attribute]] into an [[EntityReader]]
    *
    * @param attr
    *     the attribute with value type `DD` and cardinality `Card` to convert.
    * @return an entity reader that will read the value for attribute `attr`
    *     given an entity with that attribute.
    */
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderCast {

  /** If there is a conversion for `DD` to `A`
    * (see [[FromDatomic]]) then we can read an `A`
    * for an attribute with value type `DD`.
    */
  implicit def attr2EntityReaderCastOne[DD <: AnyRef, A](implicit conv: FromDatomic[DD, A]) =
    new Attribute2EntityReaderCast[DD, Cardinality.one.type, A] {
      override def convert(attr: Attribute[DD, Cardinality.one.type]) = new EntityReader[A] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            conv.from(o.asInstanceOf[DD])
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** If there is a conversion for `DD` to `A`
    * (see [[FromDatomic]]) then we can read a `Set` of `A`
    * for a many attribute with value type `DD`.
    */
  implicit def attr2EntityReaderCastMany[DD <: AnyRef, A](implicit conv: FromDatomic[DD, A]) =
    new Attribute2EntityReaderCast[DD, Cardinality.many.type, Set[A]] {
      override def convert(attr: Attribute[DD, Cardinality.many.type]) = new EntityReader[Set[A]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[A]
                val iter = coll.iterator
                while (iter.hasNext) {
                  builder += conv.from(iter.next().asInstanceOf[DD])
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[A]
        }
      }
    }


  /** Given a cardinality one reference attribute, we can read the entity id of
    * the entity that is referenced.
    *
    * If the referenced entity is an ident entity, then we can still get the
    * entity id from the ident keyword.
    */
  implicit val attr2EntityReaderCastIdOnly =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, Long] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.one.type]) = new EntityReader[Long] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case e: datomic.Entity =>
                e.get(clojure.lang.Keyword.intern("db", "id")).asInstanceOf[Long]
              case k: clojure.lang.Keyword =>
                val db = entity.entity.db()
                db.entid(k).asInstanceOf[Long]
              case _ =>
                throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
            }
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** Given a cardinality many reference attribute, we can read the set of entity ids of
    * the entities that are referenced.
    *
    * If the referenced entities are ident entities, then we can still get the
    * entity ids from the ident keywords.
    */
  implicit val attr2EntityReaderCastManyIdOnly =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[Long]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new EntityReader[Set[Long]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[Long]
                val iter = coll.iterator
                while (iter.hasNext) {
                  iter.next() match {
                    case e: datomic.Entity =>
                      builder += e.get(clojure.lang.Keyword.intern("db", "id")).asInstanceOf[Long]
                    case k: clojure.lang.Keyword =>
                      val db = entity.entity.db()
                      builder += db.entid(k).asInstanceOf[Long]
                    case _ =>
                      throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
                  }
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[Long]
        }
      }
    }


  /** Given a cardinality one reference attribute, we can read the ident keyword of
    * the ident entity that is referenced.
    *
    * And if we have an implicit view from [[Keyword]] to a type `K`, then we can
    * get the result of this view, rather than the keyword.
    *
    * If the referenced entity is not an ident entity, then an exception will be thrown.
    */
  implicit def attr2EntityReaderCastKeyword[K](implicit fromKeyword: Keyword => K) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, K] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.one.type]) = new EntityReader[K] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case k: clojure.lang.Keyword =>
                fromKeyword(k)
              case e: datomic.Entity =>
                throw new EntityMappingException("Expected an ident entity for a reference type attribute, not a regular entity")
              case _ =>
                throw new EntityMappingException("Expected an ident entity for a reference type attribute")
            }
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** Given a cardinality many reference attribute, we can read set of ident keywords of
    * the ident entities that are referenced.
    *
    * And if we have an implicit view from [[Keyword]] to a type `K`, then we can
    * get the result of this view, rather than the keyword.
    *
    * If the referenced entities are not all ident entities, then an exception will be thrown.
    */
  implicit def attr2EntityReaderCastManyKeyword[K](implicit fromKeyword: Keyword => K) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[K]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new EntityReader[Set[K]] {
        override def read(entity: Entity) = {
          val o  = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[K]
                val iter = coll.iterator
                while (iter.hasNext) {
                  iter.next() match {
                    case k: clojure.lang.Keyword =>
                      builder += fromKeyword(k)
                    case e: datomic.Entity =>
                      throw new EntityMappingException("Expected an ident entity for a reference type attribute, not a regular entity")
                    case _ =>
                      throw new EntityMappingException("Expected an ident entity for a reference type attribute")
                  }
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[K]
        }
      }
    }


  /** If there is an [[EntityReader]] for type `A`
    * then we can read the entity referenced by a
    * cardinality one reference attribute as an `A`.
    */
  implicit def attr2EntityReaderOneObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, A] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.one.type]) = new EntityReader[A] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case e: datomic.Entity =>
                er.read(new Entity(e))
              case k: clojure.lang.Keyword =>
                throw new EntityMappingException("Expected a regular entity for a reference type attribute, not an ident entity")
              case _ =>
                throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
            }
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** If there is an [[EntityReader]] for type `A`
    * then we can read set of entities referenced by a
    * cardinality many reference attribute as a `Set` of `A`.
    */
  implicit def attr2EntityReaderManyObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[A]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new EntityReader[Set[A]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[A]
                val iter = coll.iterator
                while (iter.hasNext) {
                  iter.next() match {
                    case e: datomic.Entity =>
                      builder += er.read(new Entity(e))
                    case k: clojure.lang.Keyword =>
                      throw new EntityMappingException("Expected a regular entity for a reference type attribute, not an ident entity")
                    case _ =>
                      throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
                  }
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[A]
        }
      }
    }


  /** If there is an [[EntityReader]] for type `A`
    * then we can read the entity referenced by a
    * cardinality one reference attribute as an [[IdView]] of `A`.
    */
  implicit def attr2EntityReaderOneIdView[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.one.type, IdView[A]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.one.type]) = new EntityReader[IdView[A]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case e: datomic.Entity =>
                val id = e.get(clojure.lang.Keyword.intern("db", "id")).asInstanceOf[Long]
                new IdView(er.read(new Entity(e)), id)
              case k: clojure.lang.Keyword =>
                throw new EntityMappingException("Expected a regular entity for a reference type attribute, not an ident entity")
              case _ =>
                throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
            }
          else
            throw new EntityKeyNotFoundException(attr.ident.toString)
        }
      }
    }


  /** If there is an [[EntityReader]] for type `A`
    * then we can read set of entities referenced by a
    * cardinality many reference attribute as a `Set` of [[IdView]] of `A`.
    */
  implicit def attr2EntityReaderManyIdView[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DatomicRef.type, Cardinality.many.type, Set[IdView[A]]] {
      override def convert(attr: Attribute[DatomicRef.type, Cardinality.many.type]) = new EntityReader[Set[IdView[A]]] {
        override def read(entity: Entity) = {
          val o = entity.entity.get(attr.ident)
          if (o ne null)
            o match {
              case coll: java.util.Collection[_] =>
                val builder = Set.newBuilder[IdView[A]]
                val iter = coll.iterator
                while (iter.hasNext) {
                  iter.next() match {
                    case e: datomic.Entity =>
                      val id = e.get(clojure.lang.Keyword.intern("db", "id")).asInstanceOf[Long]
                      builder += new IdView(er.read(new Entity(e)), id)
                    case k: clojure.lang.Keyword =>
                      throw new EntityMappingException("Expected a regular entity for a reference type attribute, not an ident entity")
                    case _ =>
                      throw new EntityMappingException("Expected an entity or keyword for a reference type attribute")
                  }
                }
                builder.result()
              case _ =>
                throw new EntityMappingException("Expected a collection for cardinality many attribute")
            }
          else
            Set.empty[IdView[A]]
        }
      }
    }

}
