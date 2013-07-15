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

import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI


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

  implicit val DString2String:          FromDatomicInj[DString,  String]      = FromDatomicInj(_.underlying)
  implicit val DBoolean2Boolean:        FromDatomicInj[DBoolean, Boolean]     = FromDatomicInj(_.underlying)
  implicit val DLong2Long:              FromDatomicInj[DLong,    Long]        = FromDatomicInj(_.underlying)
  implicit val DFloat2Float:            FromDatomicInj[DFloat,   Float]       = FromDatomicInj(_.underlying)
  implicit val DDouble2Double:          FromDatomicInj[DDouble,  Double]      = FromDatomicInj(_.underlying)
  implicit val DBigInt2BigInt:          FromDatomicInj[DBigInt,  BigInt]      = FromDatomicInj(_.underlying)
  implicit val DBigDec2BigDec:          FromDatomicInj[DBigDec,  BigDecimal]  = FromDatomicInj(_.underlying)
  implicit val DInstant2Date:           FromDatomicInj[DInstant, Date]        = FromDatomicInj(_.underlying)
  implicit val DUuid2UUID:              FromDatomicInj[DUuid,    UUID]        = FromDatomicInj(_.underlying)
  implicit val DUri2URI:                FromDatomicInj[DUri,     URI]         = FromDatomicInj(_.underlying)
  implicit val DBytes2Bytes:            FromDatomicInj[DBytes,   Array[Byte]] = FromDatomicInj(_.underlying)
  implicit val DKeyword2Keyword:        FromDatomicInj[DKeyword, Keyword]     = FromDatomicInj(_.underlying)

}

/**
  * A multi-valued function, or relation, from DD => T,
  * So the type T is no longer uniquely determined by DD.
  * For example, DLong maps to DLong, Long, Int, Short,
  * Char, and Byte.
  */
trait FromDatomicImplicits {

  implicit def FromDatomicInj2FromDatomic[DD <: DatomicData, T]
      (implicit fd: FromDatomicInj[DD, T]): FromDatomic[DD, T] = 
      FromDatomic[DD, T](fd.from(_))

  implicit val DLong2Int:               FromDatomic[DLong,    Int]            = FromDatomic(_.underlying.toInt)
  implicit val DLong2Char:              FromDatomic[DLong,    Short]          = FromDatomic(_.underlying.toShort)
  implicit val DLong2Short:             FromDatomic[DLong,    Char]           = FromDatomic(_.underlying.toChar)
  implicit val DLong2Byte:              FromDatomic[DLong,    Byte]           = FromDatomic(_.underlying.toByte)
  implicit val DBigInt2JBigInt:         FromDatomic[DBigInt,  JBigInt]        = FromDatomic(_.underlying.underlying)
  implicit val DBigDec2JBigDec:         FromDatomic[DBigDec,  JBigDecimal]    = FromDatomic(_.underlying.underlying)

  implicit def DD2DD[DD <: DatomicData] = FromDatomic[DD, DD]( dd => dd )

  implicit def DD2DCollTyped[T](implicit fdat: FromDatomicCast[T]): FromDatomic[DColl, Set[T]] =
    FromDatomic(_.toSet.map( fdat.from(_) ))
}

/**
  * FromDatomicCast fixes the source type
  * of FromDatomic as DatomicData
  * Trivially, is a multi-valued function
  * from DatomicData => T
  */
trait FromDatomicCastImplicits {
  implicit def FromDatomic2FromDatomicCast[DD <: DatomicData, A](implicit fdat: FromDatomic[DD, A]) = 
    FromDatomicCast{ (dd: DatomicData) => fdat.from(dd.asInstanceOf[DD]) }
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
  implicit val String2DString   = ToDatomicInj[DString,  String]      ((s: String)      => DString(s))
  implicit val Boolean2DBoolean = ToDatomicInj[DBoolean, Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DLong       = ToDatomicInj[DLong,    Long]        ((l: Long)        => DLong(l))
  implicit val Float2DFloat     = ToDatomicInj[DFloat,   Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DDouble   = ToDatomicInj[DDouble,  Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DBigInt   = ToDatomicInj[DBigInt,  BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DBigDec   = ToDatomicInj[DBigDec,  BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val Date2DDate       = ToDatomicInj[DInstant, Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DUuid       = ToDatomicInj[DUuid,    UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DUri         = ToDatomicInj[DUri,     URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DBytes     = ToDatomicInj[DBytes,   Array[Byte]] ((a: Array[Byte]) => DBytes(a))
  implicit val Keyword2DKeyword = ToDatomicInj[DKeyword, Keyword]     ((k: Keyword)     => DKeyword(k))
}

/**
  * ToDatomic extends ToDatomicInj by widening the domain
  * and also destroying the injectivity property
  * (both Long and Int map to DLong)
  * But it is still a function (unlike FromDatomic)
  */
trait ToDatomicImplicits {
  implicit def ToDatomicInj2ToDatomic[DD <: DatomicData, T]
      (implicit tdat: ToDatomicInj[DD, T]): ToDatomic[DD, T] = 
      ToDatomic[DD, T](tdat.to(_))

  implicit val Int2DLong        = ToDatomic[DLong,    Int]         ((l: Int)         => DLong(l))
  implicit val Short2DLong      = ToDatomic[DLong,    Short]       ((s: Short)       => DLong(s))
  implicit val Char2DLong       = ToDatomic[DLong,    Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DLong       = ToDatomic[DLong,    Byte]        ((b: Byte)        => DLong(b))
  implicit val JBigInt2DBigInt  = ToDatomic[DBigInt,  JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DBigDec  = ToDatomic[DBigDec,  JBigDecimal] ((i: JBigDecimal) => DBigDec(i))


  implicit def KeywordIdentified2DRef[T <: KeywordIdentified] = ToDatomic[DRef, T] { (x: T) => DRef(x.ident) }
  implicit def TempIdentified2DRef   [T <: TempIdentified]    = ToDatomic[DRef, T] { (x: T) => DRef(x.id) }
  implicit def FinalIdentified2DRef  [T <: FinalIdentified]   = ToDatomic[DRef, T] { (x: T) => DRef(x.id) }

  implicit def DDatomicData[DD <: DatomicData] = ToDatomic[DD, DD]( dd => dd )

  implicit def DColl2SetWrites[C, A](implicit witness: C <:< Iterable[A], tdat: ToDatomicCast[A]) =
    ToDatomic[DColl, C]{ (l: C) =>
      DColl(witness(l).map(tdat.to(_)))
    }

}

/**
  * ToDatomicCast fixes the return type of ToDatomic as DatomicData
  */
trait ToDatomicCastImplicits {
  implicit def DDWriter2ToDatomicCast[DD <: DatomicData, A](implicit tdat: ToDatomic[DD, A]) = 
    ToDatomicCast[A] { (a: A) => tdat.to(a): DatomicData }
}


