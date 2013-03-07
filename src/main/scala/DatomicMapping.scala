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

import scala.language.implicitConversions

import scala.util.{Try, Success, Failure}

case class IdView[T](t: T, id: Long) {
  override def toString = s"IdView($id)($t)"
}

object IdView {
  def apply[T](id: Long)(t: T) = new IdView[T](t, id)
}

sealed trait EntityMapper[A]

trait EntityReader[A] extends EntityMapper[A] {
  self => 
  def read(e: DEntity): A

  def map[B](f: A => B): EntityReader[B] = EntityReader[B] { e =>
    f(self.read(e))
  }

  def flatMap[B](f: A => EntityReader[B]): EntityReader[B] = EntityReader[B]{ e => 
    f(self.read(e)).read(e)
  }

  def orElse(other: EntityReader[A]): EntityReader[A] = EntityReader[A]{ e =>
    try {
      self.read(e)
    } catch {
      case ex: Throwable => other.read(e)
    }
  }

  def collect[B](f: PartialFunction[A, B]) = EntityReader[B]{ e =>
    val a = self.read(e)
    if(f.isDefinedAt(a)) f(a)
    else throw new EntityMappingException(s"PartialFunction not defined for value $a")
  }

  def filter(p: A => Boolean): EntityReader[A] = EntityReader[A]{ e =>
    val a = self.read(e)

    if(p(a)) a
    else throw new EntityMappingException(s"filtered value $a")
  }

  def filterNot(p: A => Boolean): EntityReader[A] = EntityReader[A]{ e =>
    val a = self.read(e)

    if(!p(a)) a
    else throw new EntityMappingException(s"filtered value $a")
  }

}

object EntityReader extends EntityReaderImplicits {
  def apply[A]( f: DEntity => A ) = new EntityReader[A] {
    def read(e: DEntity): A = f(e)
  }
}

trait PartialAddEntityWriter[A] extends EntityMapper[A] {
  def write(a: A): PartialAddEntity
}

object PartialAddEntityWriter extends PartialAddEntityWriterImplicits {
  def apply[A](f: A => PartialAddEntity) = new PartialAddEntityWriter[A] {
    def write(a: A) = f(a)
  }
}

trait Attribute2EntityReaderInj[DD <: DatomicData, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderInj extends Attribute2EntityReaderInjImplicits

trait Attribute2EntityReaderCast[DD <: DatomicData, Card <: Cardinality, T] {
  def convert(attr: Attribute[DD, Card]): EntityReader[T]
}

object Attribute2EntityReaderCast extends Attribute2EntityReaderCastImplicits

trait Attribute2PartialAddEntityWriter[DD <: DatomicData, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[Dest]
}

object Attribute2PartialAddEntityWriter extends Attribute2PartialAddEntityWriterImplicits


class AttributeOps[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
{
  def read[A](implicit a2er: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[A] =
    a2er.convert(attr)

  def readOpt[A](implicit a2er: Attribute2EntityReaderCast[DD, Card, A]): EntityReader[Option[A]] =
    EntityReader[Option[A]] { e: DEntity => 
      // searches attributes in the entity before reading it
      e.get(attr.ident) match {
        case None => None
        case Some(_) => Some(a2er.convert(attr).read(e))
      }
    }

  def write[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[A] = a2ew.convert(attr)
  def writeOpt[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[Option[A]] = 
    PartialAddEntityWriter[Option[A]] { a => a match {
      case None => PartialAddEntity.empty
      case Some(a) => a2ew.convert(attr).write(a)
    } }
}  

object DatomicMapping 
  extends CombinatorImplicits 
  with EntityReaderImplicits 
  with Attribute2EntityReaderInjImplicits
  with Attribute2EntityReaderCastImplicits
  with PartialAddEntityWriterImplicits
  with Attribute2PartialAddEntityWriterImplicits
{
  def fromEntity[A](e: DEntity)(implicit er: EntityReader[A]): A = er.read(e)

  def toEntity[A](id: DId)(a: A)(implicit ew: PartialAddEntityWriter[A]): AddEntity = AddEntity(id, ew.write(a))

  val ID = Attribute( Namespace.DB / "id", SchemaType.long, Cardinality.one)

  val readId    = new AttributeOps(ID).read[Long]   (attr2EntityReaderCastOne)
  val readIdOpt = new AttributeOps(ID).readOpt[Long](attr2EntityReaderCastOne)

  /*val writeId = new AttributeOps(ID).write[Long](attr2PartialAddEntityWriterOne)

  def writeIdOpt(partition: Partition = Partition.USER) = 
    PartialAddEntityWriter[Option[Long]] { a => a match {
      case None => attr2PartialAddEntityWriterOne.convert(ID).to(DId(partition))
      case Some(a) => attr2PartialAddEntityWriterOne.convert(ID).to(a)
    } }*/

  implicit def attributeOps[DD <: DatomicData, C <: Cardinality](attr: Attribute[DD, C]) = new AttributeOps(attr)

  implicit class toSchemaDEntityOps(override val entity: DEntity) extends SchemaDEntityOps 
}

trait EntityReaderImplicits {

  implicit object EntityReaderMonad extends Monad[EntityReader] {
    def unit[A](a: A) = EntityReader[A]{ (e: DEntity) => a }
    def bind[A, B](ma: EntityReader[A], f: A => EntityReader[B]) = 
      EntityReader[B]{ (e: DEntity) => f(ma.read(e)).read(e) }
  }

  implicit object EntityReaderFunctor extends Functor[EntityReader] {
    def fmap[A, B](ereader: EntityReader[A], f: A => B) = EntityReader{ e => f(ereader.read(e)) }
  }

  implicit val DEntityReader: EntityReader[DEntity] = EntityReader{ e: DEntity => e }
}

trait Attribute2EntityReaderCastImplicits {

  implicit def attr2EntityReaderCastOne[DD <: DatomicData, A](implicit fdat: FromDatomic[DD, A]) =
      new Attribute2EntityReaderCast[DD, CardinalityOne.type, A] {
        def convert(attr: Attribute[DD, CardinalityOne.type]): EntityReader[A] =
          EntityReader { entity =>
            val dd = entity(attr.ident).asInstanceOf[DD]
            fdat.from(dd)
          }
      }

  implicit def attr2EntityReaderCastMany[DD <: DatomicData, A](implicit fdat: FromDatomic[DD, A]) =
  new Attribute2EntityReaderCast[DD, CardinalityMany.type, Set[A]] {
    def convert(attr: Attribute[DD, CardinalityMany.type]): EntityReader[Set[A]] =
      EntityReader { entity =>
        entity.get(attr.ident) map { case DSet(elems) =>
          elems map { elem => fdat.from(elem.asInstanceOf[DD]) }
        } getOrElse (Set.empty)
      }
  }


  implicit val attr2EntityReaderCastIdOnly =
    new Attribute2EntityReaderCast[DRef, CardinalityOne.type, Long] {
      def convert(attr: Attribute[DRef, CardinalityOne.type]): EntityReader[Long] =
        EntityReader { entity =>
          entity(attr.ident).asInstanceOf[DEntity].id
        }
    }  

  implicit val attr2EntityReaderCastManyIdOnly =
    new Attribute2EntityReaderCast[DRef, CardinalityMany.type, Set[Long]] {
      def convert(attr: Attribute[DRef, CardinalityMany.type]): EntityReader[Set[Long]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DSet(elems) =>
            elems map {
              case subent: DEntity => subent.id
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            }
          } getOrElse (Set.empty)
        }
    }

  /*
   * we need to have an entity reader for type A in scope
   * we can read the ref value of an attribute as an entity
   * and then use the entity reader to interpet it
   */
  implicit def attr2EntityReaderOneObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, CardinalityOne.type, A] {
      def convert(attr: Attribute[DRef, CardinalityOne.type]): EntityReader[A] =
        EntityReader { entity =>
          val subent = entity(attr.ident).asInstanceOf[DEntity]
          er.read(subent)
        }
    }
  // similarly for multi-valued attributes
  implicit def attr2EntityReaderManyObj[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, CardinalityMany.type, Set[A]] {
      def convert(attr: Attribute[DRef, CardinalityMany.type]): EntityReader[Set[A]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DSet(elems) =>
            elems map {
              case subent: DEntity => er.read(subent)
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            }
          } getOrElse (Set.empty)
        }
    }

  implicit def attr2EntityReaderOneRef[A](implicit witness: A <:!< DRef, er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, CardinalityOne.type, IdView[A]] {
      def convert(attr: Attribute[DRef, CardinalityOne.type]): EntityReader[IdView[A]] =
        EntityReader { entity =>
          val subent = entity(attr.ident).asInstanceOf[DEntity]
          IdView(subent.id)(er.read(subent))
        }
    }  

  implicit def attr2EntityReaderManyRef[A](implicit witness: A <:!< DRef, er: EntityReader[A]) =
    new Attribute2EntityReaderCast[DRef, CardinalityMany.type, Set[IdView[A]]] {
      def convert(attr: Attribute[DRef, CardinalityMany.type]): EntityReader[Set[IdView[A]]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DSet(elems) =>
            elems map {
              case subent: DEntity => IdView(subent.id)(er.read(subent))
              case _ => throw new EntityMappingException("expected DatomicData to be DEntity")
            }
          } getOrElse (Set.empty)
        }
    }

}

trait Attribute2EntityReaderInjImplicits {

  /*
   * The values of reference attributes may be other entities,
   * or they may be idents. We have to be conservative and
   * return DatomicData so that the user can determine the
   * precise type.
   */
  implicit val attr2EntityReaderDRef2DD =
    new Attribute2EntityReaderInj[DRef, CardinalityOne.type, DatomicData] {
      def convert(attr: Attribute[DRef, CardinalityOne.type]): EntityReader[DatomicData] =
        EntityReader { entity => entity(attr.ident) }
    }
  // similarly for multi-valued attributes
  implicit val attr2EntityReaderManyDRef2DD =
    new Attribute2EntityReaderInj[DRef, CardinalityMany.type, Set[DatomicData]] {
      def convert(attr: Attribute[DRef, CardinalityMany.type]): EntityReader[Set[DatomicData]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DSet(elems) => elems } getOrElse (Set.empty)
        }
    }

  /*
   * the given attribute determines the subtype of DatomicData
   * and from that subtype, FromDatomicInj uniquely determines
   * the result type A
   */
  implicit def attr2EntityReaderOne[DD <: DatomicData, A](implicit fdat: FromDatomicInj[DD, A]) = 
    new Attribute2EntityReaderInj[DD, CardinalityOne.type, A] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): EntityReader[A] =
        EntityReader { entity =>
          val dd = entity(attr.ident).asInstanceOf[DD]
          fdat.from(dd)
        }
    }  
  // similarly for multi-valued attributes
  implicit def attr2EntityReaderMany[DD <: DatomicData, A](implicit fdat: FromDatomicInj[DD, A]) = 
    new Attribute2EntityReaderInj[DD, CardinalityMany.type, Set[A]] {
      def convert(attr: Attribute[DD, CardinalityMany.type]): EntityReader[Set[A]] =
        EntityReader { entity =>
          entity.get(attr.ident) map { case DSet(elems) =>
            elems map { elem => fdat.from(elem.asInstanceOf[DD]) }
          } getOrElse (Set.empty)
        }
    }

}

trait PartialAddEntityWriterImplicits {

  implicit object AddEntityWriterCombinator extends Combinator[PartialAddEntityWriter] {
    def apply[A, B](ma: PartialAddEntityWriter[A], mb: PartialAddEntityWriter[B]): PartialAddEntityWriter[A ~ B] = 
      new PartialAddEntityWriter[A ~ B] {
        def write(ab: A ~ B): PartialAddEntity = ab match {
          case a ~ b => ma.write(a) ++ mb.write(b)
        }
      }
  }

  implicit object PartialAddEntityWriterContraFunctor extends ContraFunctor[PartialAddEntityWriter] {
    def contramap[A, B](w: PartialAddEntityWriter[A], f: B => A) = PartialAddEntityWriter{ b => w.write(f(b)) }
  }

}

trait Attribute2PartialAddEntityWriterImplicits {

  implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData, Source](implicit tdat: ToDatomic[DD, Source]) =
    new Attribute2PartialAddEntityWriter[DD, CardinalityOne.type, Source] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): PartialAddEntityWriter[Source] = {
        PartialAddEntityWriter[Source]{ s: Source =>
          PartialAddEntity( Map( attr.ident -> tdat.to(s) ) )
        }
      }
    }  


  implicit def attr2PartialAddEntityWriterMany[DD <: DatomicData, Source](implicit tdat: ToDatomic[DSet, Set[Source]]) =
    new Attribute2PartialAddEntityWriter[DD, CardinalityMany.type, Set[Source]] {
      def convert(attr: Attribute[DD, CardinalityMany.type]): PartialAddEntityWriter[Set[Source]] = {
        PartialAddEntityWriter[Set[Source]]{ s: Set[Source] =>
          if (s.isEmpty) PartialAddEntity( Map.empty )
          else PartialAddEntity( Map( attr.ident -> tdat.to(s) ) )
        }
      }
    }

  /*implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData] = 
    new Attribute2PartialAddEntityWriter[DD, CardinalityOne.type, DD] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): PartialAddEntityWriter[DD] = {
        PartialAddEntityWriter[DD]{ d: DD => 
          PartialAddEntity( Map( attr.ident -> d ) )
        }
      }
    }*/

}