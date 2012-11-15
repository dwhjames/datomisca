package reactivedatomic

import scala.util.{Try, Success, Failure}

case class Ref[T](ref: T, id: DId) {
  override def toString = s"Ref($id)($ref)"
}

object Ref {
  def apply[T](theId: DId)(t: T) = new Ref[T](t, theId)
}


trait EntityReader[A] {
  self => 
  def read(e: datomic.Entity): Try[A]

  def map[B](f: A => B): EntityReader[B] = new EntityReader[B] {
    def read(e: datomic.Entity): Try[B] = self.read(e).map(f(_))
  }

  def flatMap[B](f: A => EntityReader[B]): EntityReader[B] = new EntityReader[B] {
    def read(e: datomic.Entity): Try[B] = self.read(e).flatMap( a => f(a).read(e) )
  }
}

object EntityReader{
  def apply[A]( f: datomic.Entity => Try[A] ) = new EntityReader[A] {
    def read(e: datomic.Entity): Try[A] = f(e)
  }
}

trait PartialAddEntityWriter[A] {
  def write(a: A): PartialAddEntity
}

object PartialAddEntityWriter{
  def apply[A](f: A => PartialAddEntity) = new PartialAddEntityWriter[A] {
    def write(a: A) = f(a)
  }

}

trait DatomicEntityFormat {
  def fromDatomic[A](e: datomic.Entity)(implicit er: EntityReader[A]) = er.read(e)
}

trait Attribute2EntityReader[DD <: DatomicData, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): EntityReader[Dest]
}

trait Attribute2PartialAddEntityWriter[DD <: DatomicData, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[Dest]
}

class AttributeOps[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card])
{
  def read[A](implicit a2er: Attribute2EntityReader[DD, Card, A]): EntityReader[A] = a2er.convert(attr)
  def readOpt[A](implicit a2er: Attribute2EntityReader[DD, Card, A]): EntityReader[Option[A]] = 
    EntityReader[Option[A]] { e: datomic.Entity => 
      // searches attributes in the entity before reading it
      Option(e.get(attr.ident.toString)) match {
        case None => Success(None)
        case Some(_) => a2er.convert(attr).read(e).map(Some(_))
      }
    }

  def write[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[A] = a2ew.convert(attr)
  def writeOpt[A](implicit a2ew: Attribute2PartialAddEntityWriter[DD, Card, A]): PartialAddEntityWriter[Option[A]] = 
    PartialAddEntityWriter[Option[A]] { a => a match {
      case None => PartialAddEntity.empty
      case Some(a) => a2ew.convert(attr).write(a)
    } }
}  

object EntityImplicits extends EntityReaderImplicits with CombinatorImplicits with EntityWriterImplicits {
  def fromEntity[A](e: datomic.Entity)(implicit er: EntityReader[A]) = er.read(e)

  def toAddEntity[A](id: DId)(a: A)(implicit ew: PartialAddEntityWriter[A]) = AddEntity(id, ew.write(a))
}

trait EntityReaderImplicits {
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  implicit object EntityReaderMonad extends Monad[EntityReader] {
    def unit[A](a: A) = EntityReader[A]{ (e: datomic.Entity) => Success(a) }
    def bind[A, B](ma: EntityReader[A], f: A => EntityReader[B]) = 
      EntityReader[B]{ (e: datomic.Entity) => ma.read(e).flatMap(a => f(a).read(e)) }
  }

  implicit object EntityReaderFunctor extends Functor[EntityReader] {
    def fmap[A, B](ereader: EntityReader[A], f: A => B) = EntityReader{ e => ereader.read(e).map(f) }
  }

  implicit def attr2EntityReaderOneRef[A](implicit er: EntityReader[A]) =
    new Attribute2EntityReader[DRef, CardinalityOne.type, Ref[A]] {
      def convert(attr: Attribute[DRef, CardinalityOne.type]): EntityReader[Ref[A]] = {
        EntityReader[Ref[A]]{ e: datomic.Entity => 
          try {
            Option(e.get(attr.ident.toString)) match {
              case None => Failure(new RuntimeException(attr.ident.toString + " not found"))
              case Some(value) => 
                val subent = value.asInstanceOf[datomic.Entity]
                val id = subent.get(Keyword("id", Namespace.DB).toNative).asInstanceOf[Long]
                er.read(subent).map{ a: A => Ref(DId(id))(a) }
            }
          }catch{
            case e: Throwable => Failure(e)
          }
        }
      }
    }  

  implicit def attr2EntityReaderManyRef[A](implicit er: EntityReader[A]) = 
    new Attribute2EntityReader[DRef, CardinalityMany.type, Set[Ref[A]]] {
      def convert(attr: Attribute[DRef, CardinalityMany.type]): EntityReader[Set[Ref[A]]] = {
        EntityReader[Set[Ref[A]]]{ e: datomic.Entity => 
          try {
            Option(e.get(attr.ident.toString)) match {
              case None => Failure(new RuntimeException(attr.ident.toString + " not found"))
              case Some(value) => 
                val l = value.asInstanceOf[java.util.Collection[java.lang.Object]].toSet.map{ e: java.lang.Object =>

                  val subent = e.asInstanceOf[datomic.Entity]
                  val id = subent.get(Keyword("id", Namespace.DB).toNative).asInstanceOf[Long]
                  er.read(subent).map{ a: A => Ref(DId(id))(a) }
                }

                Utils.sequence(l)
            }
          }catch{
            case e: Throwable => Failure(e)
          }
        }
      }
    }

  implicit def attr2EntityReaderOne[DD <: DatomicData, Dest](implicit ddr: DDReader[DD, Dest]) = 
    new Attribute2EntityReader[DD, CardinalityOne.type, Dest] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): EntityReader[Dest] = {
        EntityReader[Dest]{ e: datomic.Entity => 
          try {
            Success(
              ddr.read( DatomicData.toDatomicData( e.get(attr.ident.toString) ).asInstanceOf[DD] )
            )
          }catch{
            case e: Throwable => Failure(e)
          }
        }
      }
    }  


  implicit def attr2EntityReaderMany[DD <: DatomicData, Dest](implicit ddr: DDReader[DD, Dest]) = 
    new Attribute2EntityReader[DD, CardinalityMany.type, Set[Dest]] {
      def convert(attr: Attribute[DD, CardinalityMany.type]): EntityReader[Set[Dest]] = {
        EntityReader[Set[Dest]]{ e: datomic.Entity => 
          try {
            Success(
              e.get(attr.ident.toString).asInstanceOf[java.util.Collection[java.lang.Object]].toSet.map{ e: java.lang.Object  => 
                ddr.read(DatomicData.toDatomicData(e).asInstanceOf[DD])
              }
            )
          }catch{
            case e: Throwable => Failure(e)
          }
        }
      }
    }

  implicit def attributeOps[DD <: DatomicData, C <: Cardinality](attr: Attribute[DD, C]) = new AttributeOps(attr)

}

trait EntityWriterImplicits {
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

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

  implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData, Dest](implicit ddw: DDWriter[DD, Dest]) = 
    new Attribute2PartialAddEntityWriter[DD, CardinalityOne.type, Dest] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): PartialAddEntityWriter[Dest] = {
        PartialAddEntityWriter[Dest]{ d: Dest => 
          PartialAddEntity( Map( attr.ident -> ddw.write(d) ) )
        }
      }
    }  


  implicit def attr2PartialAddEntityWriterMany[DD <: DatomicData, Dest](implicit ddw: DDWriter[DSet, Set[Dest]]) = 
    new Attribute2PartialAddEntityWriter[DD, CardinalityMany.type, Set[Dest]] {
      def convert(attr: Attribute[DD, CardinalityMany.type]): PartialAddEntityWriter[Set[Dest]] = {
        PartialAddEntityWriter[Set[Dest]]{ d: Set[Dest] => 
          PartialAddEntity( Map( attr.ident -> ddw.write(d) ) )              
        }
      }
    }

}