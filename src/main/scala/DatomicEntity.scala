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

import scala.util.{Try, Success, Failure}


class DEntity(val entity: datomic.Entity) extends DatomicData {
  def toNative = entity

  def id: Long = as[Long](Namespace.DB / "id")

  def touch() = {
    entity.touch()
    this
  }

  def apply(keyword: Keyword): DatomicData =
    Option {
      entity.get(keyword.toNative)
    } match {
      case None => throw new EntityKeyNotFoundException(keyword)
      case Some(value) => Datomic.toDatomicData(value)
    }

  def get(keyword: Keyword): Option[DatomicData] =
    try {
      Some(apply(keyword))
    } catch {
      case _: EntityKeyNotFoundException => None
    }

  def as[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): T =
    reader.read(apply(keyword))

  
  def getAs[T](keyword: Keyword)(implicit reader: DDReader[DatomicData, T]): Option[T] =
    try {
      Some(as(keyword))
    } catch {
      case _: EntityKeyNotFoundException => None
    }

  def keySet: Set[Keyword] = {
    import scala.collection.JavaConverters._

    entity.keySet.asScala.view.map{ key: Any =>
      Keyword(key.asInstanceOf[clojure.lang.Keyword])
    }.toSet
  }

  def toMap: Map[Keyword, DatomicData] =
    keySet.view.map{ key: Keyword =>
      (key -> apply(key))
    }.toMap

  /* extension with attributes */
  /*def as[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])
  (implicit dd2dd: DD2DDReader[DD], dd2t: DD2ScalaReader[DD, T]): T = {
    dd2t.read(dd2dd.read(apply(attr.ident)))
  }*/

  def get[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Option[T] = {
    Try { attrC.convert(attr).read(this) }.toOption
  }

  def tryGet[DD <: DatomicData, Card <: Cardinality, T](attr: Attribute[DD, Card])(implicit attrC: Attribute2EntityReader[DD, Card, T]): Try[T] = {
    Try { attrC.convert(attr).read(this) }
  }

  def getRef[T](attr: Attribute[DRef, CardinalityOne.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Option[Ref[T]] = {
    tryGet(attr).toOption
  }

  def getRefs[T](attr: Attribute[DRef, CardinalityMany.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Option[Set[Ref[T]]] = {
    tryGet(attr).toOption
  }

  def tryGetRef[T](attr: Attribute[DRef, CardinalityOne.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Try[Ref[T]] = {
    tryGet(attr)
  }

  def tryGetRefs[T](attr: Attribute[DRef, CardinalityMany.type])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Try[Set[Ref[T]]] = {
    tryGet(attr)
  }

  def get[T](attr: RefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Option[Ref[T]] = {
    tryGet(attr).toOption
  }

  def tryGet[T](attr: RefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityOne.type, Ref[T]]): Try[Ref[T]] = {
    Try { attrC.convert(attr).read(this) }
  }

  def get[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Option[Set[Ref[T]]] = {
    tryGet(attr).toOption
  }

  def tryGet[T](attr: ManyRefAttribute[T])(implicit attrC: Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[T]]]): Try[Set[Ref[T]]] = {
    Try { attrC.convert(attr).read(this) }
  }

}

object DEntity {
  def apply(ent: datomic.Entity) = new DEntity(ent)
}
