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

import java.{lang => jl}
import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.{util => ju}
import java.util.{Date, UUID}
import java.net.URI

import clojure.{lang => clj}


/**
  * Think of FromDatomicInj[DD, T] as a type-level function: DD => T
  * The implicits here construct a multi-parameter type class,
  * and there is a functional dependency from DD to T: DD uniquely
  * determines T. In fact, this is an injective function, as there
  * is at most one FromDatomicInj for each DatomicData subtype, and each
  * map to distinct Scala/Java types. As a consequence, its inverse
  * is a partial function.
  */
private[datomisca] trait FromDatomicInjImplicits {

  implicit val DString2String:          FromDatomicInj[String,      String]      = FromDatomicInj(identity)
  implicit val DBoolean2Boolean:        FromDatomicInj[jl.Boolean,  Boolean]     = FromDatomicInj(b => b)
  implicit val DLong2Long:              FromDatomicInj[jl.Long,     Long]        = FromDatomicInj(l => l)
  implicit val DDouble2Double:          FromDatomicInj[jl.Double,   Double]      = FromDatomicInj(d => d)
  implicit val DFloat2Float:            FromDatomicInj[jl.Float,    Float]       = FromDatomicInj(f => f)
  implicit val DBigInt2BigInt:          FromDatomicInj[JBigInt,     BigInt]      = FromDatomicInj(i => new BigInt(i))
  implicit val DBigDec2BigDec:          FromDatomicInj[JBigDecimal, BigDecimal]  = FromDatomicInj(d => new BigDecimal(d))
  implicit val DInstant2Date:           FromDatomicInj[Date,        Date]        = FromDatomicInj(identity)
  implicit val DUuid2UUID:              FromDatomicInj[UUID,        UUID]        = FromDatomicInj(identity)
  implicit val DUri2URI:                FromDatomicInj[URI,         URI]         = FromDatomicInj(identity)
  implicit val DBytes2Bytes:            FromDatomicInj[Array[Byte], Array[Byte]] = FromDatomicInj(identity)
  implicit val DKeyword2Keyword:        FromDatomicInj[Keyword, Keyword]         = FromDatomicInj(identity)

  implicit val entity2Entity: FromDatomicInj[datomic.Entity, Entity] = FromDatomicInj(e => new Entity(e))

}

/**
  * A multi-valued function, or relation, from DD => T,
  * So the type T is no longer uniquely determined by DD.
  * For example, DLong maps to DLong, Long, Int, Short,
  * Char, and Byte.
  */
trait FromDatomicImplicits {

  implicit def FromDatomicInj2FromDatomic[DD <: AnyRef, T]
      (implicit fd: FromDatomicInj[DD, T]): FromDatomic[DD, T] = 
      FromDatomic[DD, T](fd.from(_))

  implicit val DLong2Int:               FromDatomic[jl.Long,     Int]         = FromDatomic(_.toInt)
  implicit val DLong2Char:              FromDatomic[jl.Long,     Short]       = FromDatomic(_.toShort)
  implicit val DLong2Short:             FromDatomic[jl.Long,     Char]        = FromDatomic(_.toChar)
  implicit val DLong2Byte:              FromDatomic[jl.Long,     Byte]        = FromDatomic(_.toByte)
  implicit val DBigInt2JBigInt:         FromDatomic[JBigInt,  JBigInt]     = FromDatomic(identity)
  implicit val DBigDec2JBigDec:         FromDatomic[JBigDecimal, JBigDecimal] = FromDatomic(identity)

  // implicit def DD2DD[DD <: DatomicData] = FromDatomic[DD, DD]( dd => dd )

  implicit def JavaSetToScalaSet[T](implicit conv: FromDatomicCast[T]): FromDatomic[ju.Set[AnyRef], Set[T]] = new FromDatomic[ju.Set[AnyRef], Set[T]] {
    override def from(l: ju.Set[AnyRef]) = {
      val builder = Set.newBuilder[T]
      val iter = l.iterator
      while (iter.hasNext) {
        builder += conv.from(iter.next())
      }
      builder.result
    }
  }

  implicit def JavaListToScalaSeq[T](implicit conv: FromDatomicCast[T]): FromDatomic[ju.List[AnyRef], Seq[T]] = new FromDatomic[ju.List[AnyRef], Seq[T]] {
    override def from(l: ju.List[AnyRef]) = {
      val builder = Seq.newBuilder[T]
      val iter = l.iterator
      while (iter.hasNext) {
        builder += conv.from(iter.next())
      }
      builder.result
    }
  }

}

/**
  * FromDatomicCast fixes the source type
  * of FromDatomic as DatomicData
  * Trivially, is a multi-valued function
  * from DatomicData => T
  */
trait FromDatomicCastImplicits {
  implicit def FromDatomic2FromDatomicCast[DD <: AnyRef, A](implicit fdat: FromDatomic[DD, A]) =
    FromDatomicCast{ (dd: Any) => fdat.from(dd.asInstanceOf[DD]) }
}


/**
  * Think of ToDatomicInj[DD, T] as a type-level function: T => DD
  * The implicits here construct a multi-parameter type class,
  * and there is a functional dependency from T to DD: T uniquely
  * determines DD.  In fact, this is an injective function, as there
  * is at most one ToDatomicInj for any Scala type, and each
  * map to distinct DatomicData subtypes. As a consequence, its inverse
  * is a partial function.
  */
trait ToDatomicInjImplicits {
  implicit val String2DString   = ToDatomicInj[String,  String](identity)
  implicit val Boolean2DBoolean = ToDatomicInj[jl.Boolean, Boolean](identity)
  implicit val Long2DLong       = ToDatomicInj[jl.Long,    Long](identity)
  implicit val Double2DDouble   = ToDatomicInj[jl.Double,  Double](identity)
  implicit val Float2DFloat     = ToDatomicInj[jl.Float,   Float](identity)
  implicit val BigInt2DBigInt   = ToDatomicInj[JBigInt,  BigInt]((i: BigInt) => i.bigInteger)
  implicit val BigDec2DBigDec   = ToDatomicInj[JBigDecimal, BigDecimal]((i: BigDecimal) => i.bigDecimal)
  implicit val Date2DDate       = ToDatomicInj[Date, Date](identity)
  implicit val UUID2DUuid       = ToDatomicInj[UUID,    UUID](identity)
  implicit val URI2DUri         = ToDatomicInj[URI,     URI](identity)
  implicit val Bytes2DBytes     = ToDatomicInj[Array[Byte],   Array[Byte]](identity)
  implicit val Keyword2DKeyword = ToDatomicInj[Keyword, Keyword](identity)

}

/**
  * ToDatomic extends ToDatomicInj by widening the domain
  * and also destroying the injectivity property
  * (both Long and Int map to DLong)
  * But it is still a function (unlike FromDatomic)
  */
trait ToDatomicImplicits {
  implicit def ToDatomicInj2ToDatomic[DD <: AnyRef, T]
      (implicit tdat: ToDatomicInj[DD, T]): ToDatomic[DD, T] = 
      ToDatomic[DD, T](tdat.to(_))

  implicit val Int2DLong        = ToDatomic[jl.Long, Int](_.toLong)
  implicit val Short2DLong      = ToDatomic[jl.Long, Short](_.toLong)
  implicit val Char2DLong       = ToDatomic[jl.Long, Char](_.toLong)
  implicit val Byte2DLong       = ToDatomic[jl.Long, Byte](_.toLong)
  implicit val JBigInt2DBigInt  = ToDatomic[JBigInt,  JBigInt](identity)
  implicit val JBigDec2DBigDec  = ToDatomic[JBigDecimal,  JBigDecimal](identity)


  implicit def DColl2SetWrites[C, A](implicit ev: C <:< Traversable[A], conv: ToDatomicCast[A]) = new ToDatomic[ju.List[AnyRef], C] {
    override def to(c: C) = {
      val builder = Seq.newBuilder[AnyRef]
      for (e <- c) builder += conv.to(e)
      datomic.Util.list(builder.result: _*).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit val dbConv = ToDatomic[datomic.Database, Database](_.underlying)
  implicit val datomConv = ToDatomic[datomic.Datom, Datom](_.underlying)
  implicit val rulesConv = ToDatomic[clojure.lang.IPersistentCollection, QueryRules](_.edn)

}

/**
  * ToDatomicCast fixes the return type of ToDatomic as DatomicData
  */
trait ToDatomicCastImplicits {
  implicit def DDWriter2ToDatomicCast[DD <: AnyRef, A](implicit tdat: ToDatomic[DD, A]) = 
    ToDatomicCast[A] { (a: A) => tdat.to(a): AnyRef }

  implicit def DIdCast[I <: DId] = ToDatomicCast[I] { (i: I) => i.toDatomicId }
  implicit def KeywordIdentified2DRef[I <: KeywordIdentified] = ToDatomicCast[I] { (i: I) => i.ident }
  implicit def TempIdentified2DRef   [I <: TempIdentified]    = ToDatomicCast[I] { (i: I) => i.id.toDatomicId }
  implicit def FinalIdentified2DRef  [I <: FinalIdentified]   = ToDatomicCast[I] { (i: I) => i.id.toDatomic }

  implicit val JavaListCast = ToDatomicCast[ju.List[AnyRef]](identity)
}


