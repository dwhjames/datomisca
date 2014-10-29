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

import functional._


sealed trait EntityMapper[A]

@implicitNotFound("Cannot find a Datomic entity reader for type ${A}. Consider implementing an instance of the EntityReader type class.")
trait EntityReader[A] extends EntityMapper[A] {
  self => 
  def read(e: Entity): A

  def andThen[B](that: EntityReader[B])(implicit ev: A <:< Entity): EntityReader[B] =
    EntityReader[B] { e =>
      val a = self.read(e)
      that.read(ev(a))
    }

  def andThenAll[B](that: EntityReader[B])(implicit ev: A <:< Traversable[Entity]): EntityReader[Traversable[B]] =
    EntityReader[Traversable[B]] { e =>
      val a = self.read(e)
      val coll = ev(a)
      coll.map(that.read(_))
    }

  def andThenFlatten[B](that: EntityReader[Traversable[B]])(implicit ev: A <:< Traversable[Entity]): EntityReader[Traversable[B]] =
    EntityReader[Traversable[B]] { e =>
      val a = self.read(e)
      val coll = ev(a)
      coll.flatMap(that.read(_))
    }

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

object EntityReader {
  def apply[A]( f: Entity => A ) = new EntityReader[A] {
    def read(e: Entity): A = f(e)
  }

  implicit object EntityReaderMonad extends Monad[EntityReader] {
    def unit[A](a: A) = EntityReader[A]{ (e: Entity) => a }
    def bind[A, B](ma: EntityReader[A], f: A => EntityReader[B]) = 
      EntityReader[B]{ (e: Entity) => f(ma.read(e)).read(e) }
  }

  implicit object EntityReaderFunctor extends Functor[EntityReader] {
    def fmap[A, B](ereader: EntityReader[A], f: A => B) = EntityReader{ e => f(ereader.read(e)) }
  }

  implicit val entityReader: EntityReader[Entity] = EntityReader{ e: Entity => e }
}


@implicitNotFound("Cannot find a Datomic entity writer for type ${A}. Consider implementing an instance of the PartialAddEntityWriter type class.")
trait PartialAddEntityWriter[A] extends EntityMapper[A] {
  def write(a: A): PartialAddEntity
}

object PartialAddEntityWriter {
  def apply[A](f: A => PartialAddEntity) = new PartialAddEntityWriter[A] {
    def write(a: A) = f(a)
  }

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
