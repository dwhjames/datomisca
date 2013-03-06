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

object DatomicDataImplicits
  extends DatomicDataReaderImplicits
  with    DDReaderImplicits
  with    DatomicDataWriterImplicits
  with    DDWriterCoreImplicits
  with    DDWriterImplicits

trait DatomicDataReaderImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData] = DatomicDataReader[DD](_.asInstanceOf[DD])

  implicit val DatomicData2String:  DatomicDataReader[String]      = DatomicDataReader(_.asInstanceOf[DString] .underlying)
  implicit val DatomicData2Boolean: DatomicDataReader[Boolean]     = DatomicDataReader(_.asInstanceOf[DBoolean].underlying)
  implicit val DatomicData2Long:    DatomicDataReader[Long]        = DatomicDataReader(_.asInstanceOf[DLong]   .underlying)
  implicit val DatomicData2Float:   DatomicDataReader[Float]       = DatomicDataReader(_.asInstanceOf[DFloat]  .underlying)
  implicit val DatomicData2Double:  DatomicDataReader[Double]      = DatomicDataReader(_.asInstanceOf[DDouble] .underlying)
  implicit val DatomicData2BigInt:  DatomicDataReader[BigInt]      = DatomicDataReader(_.asInstanceOf[DBigInt] .underlying)
  implicit val DatomicData2BigDec:  DatomicDataReader[BigDecimal]  = DatomicDataReader(_.asInstanceOf[DBigDec] .underlying)
  implicit val DatomicData2Date:    DatomicDataReader[Date]        = DatomicDataReader(_.asInstanceOf[DInstant].underlying)
  implicit val DatomicData2UUID:    DatomicDataReader[UUID]        = DatomicDataReader(_.asInstanceOf[DUuid]   .underlying)
  implicit val DatomicData2URI:     DatomicDataReader[URI]         = DatomicDataReader(_.asInstanceOf[DUri]    .underlying)
  implicit val DatomicData2Bytes:   DatomicDataReader[Array[Byte]] = DatomicDataReader(_.asInstanceOf[DBytes]  .underlying)

  implicit def DatomicData2DSetTyped[T](implicit reader: DatomicDataReader[T]): DatomicDataReader[Set[T]] =
    DatomicDataReader(_.asInstanceOf[DSet].toSet.map( reader.read(_) ))
}

trait DDReaderImplicits {

  /*
   * Think of DDReader[DD, T] as a type-level function: DD => T
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from DD to T: DD uniquely
   * determines T. In fact, this is an injective function, as there
   * is at most one DDReader for each DatomicData subtype, and each
   * map to distinct Scala/Java types. As a consequence, its inverse
   * is a partial function.
   */

  implicit val ReadDRef:    DDReader[DRef, DRef]    = DDReader( dd => dd )
  //implicit val ReadDEntity: DDReader[DEntity, DEntity] = DDReader( dd => dd )

  implicit val DString2String:          DDReader[DString,  String]      = DDReader(_.underlying)
  implicit val DBoolean2Boolean:        DDReader[DBoolean, Boolean]     = DDReader(_.underlying)
  implicit val DLong2Long:              DDReader[DLong,    Long]        = DDReader(_.underlying)
  implicit val DFloat2Float:            DDReader[DFloat,   Float]       = DDReader(_.underlying)
  implicit val DDouble2Double:          DDReader[DDouble,  Double]      = DDReader(_.underlying)
  implicit val DBigInt2BigInt:          DDReader[DBigInt,  BigInt]      = DDReader(_.underlying)
  implicit val DBigDec2BigDec:          DDReader[DBigDec,  BigDecimal]  = DDReader(_.underlying)
  implicit val DInstant2Date:           DDReader[DInstant, Date]        = DDReader(_.underlying)
  implicit val DUuid2UUID:              DDReader[DUuid,    UUID]        = DDReader(_.underlying)
  implicit val DUri2URI:                DDReader[DUri,     URI]         = DDReader(_.underlying)
  implicit val DBytes2Bytes:            DDReader[DBytes,   Array[Byte]] = DDReader(_.underlying)

}

trait DatomicDataWriterImplicits {

  implicit val String2DatomicData  = DatomicDataWriter[String]      ((s: String)      => DString(s))
  implicit val Boolean2DatomicData = DatomicDataWriter[Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DatomicData    = DatomicDataWriter[Long]        ((l: Long)        => DLong(l))
  implicit val Int2DatomicData     = DatomicDataWriter[Int]         ((i: Int)         => DLong(i))
  implicit val Short2DatomicData   = DatomicDataWriter[Short]       ((s: Short)       => DLong(s))
  implicit val Char2DatomicData    = DatomicDataWriter[Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DatomicData    = DatomicDataWriter[Byte]        ((b: Byte)        => DLong(b))
  implicit val Float2DatomicData   = DatomicDataWriter[Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DatomicData  = DatomicDataWriter[Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DatomicData  = DatomicDataWriter[BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DatomicData  = DatomicDataWriter[BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val JBigInt2DatomicData = DatomicDataWriter[JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DatomicData = DatomicDataWriter[JBigDecimal] ((i: JBigDecimal) => DBigDec(i))
  implicit val Date2DatomicData    = DatomicDataWriter[Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DatomicData    = DatomicDataWriter[UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DatomicData     = DatomicDataWriter[URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DatomicData   = DatomicDataWriter[Array[Byte]] ((a: Array[Byte]) => DBytes(a))

  implicit def Referenceable2DatomicData[A <: Referenceable] = DatomicDataWriter[A]{ (a: A) => a.ref }
  //implicit val DRef2DatomicData          = DatomicDataWriter[DRef]          ((d: DRef) => d)

  implicit def DDatomicData[DD <: DatomicData] = DatomicDataWriter[DD]( dd => dd.asInstanceOf[DD] )

  implicit def DD2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DatomicDataWriter[C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DD2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DatomicDataWriter[A]) =
    DatomicDataWriter[C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }

}

trait DDWriterCoreImplicits {

  /*
   * Think of DDWriterCore[DD, T] as a type-level function: T => DD
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from T to DD: T uniquely
   * determines DD.  In fact, this is an injective function, as there
   * is at most one DDWriterCore for any Scala type, and each
   * map to distinct DatomicData subtypes. As a consequence, its inverse
   * is a partial function.
   */

  implicit val String2DString   = DDWriterCore[DString,  String]      ((s: String)      => DString(s))
  implicit val Boolean2DBoolean = DDWriterCore[DBoolean, Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DLong       = DDWriterCore[DLong,    Long]        ((l: Long)        => DLong(l))
  implicit val Float2DFloat     = DDWriterCore[DFloat,   Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DDouble   = DDWriterCore[DDouble,  Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DBigInt   = DDWriterCore[DBigInt,  BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DBigDec   = DDWriterCore[DBigDec,  BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val Date2DDate       = DDWriterCore[DInstant, Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DUuid       = DDWriterCore[DUuid,    UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DUri         = DDWriterCore[DUri,     URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DBytes     = DDWriterCore[DBytes,   Array[Byte]] ((a: Array[Byte]) => DBytes(a))

  implicit val WriteDRef        = DDWriterCore[DRef,     DRef]        ((d: DRef)        => d)

}

trait DDWriterImplicits {

  /*
   * Think of DDWriter[DD, T] as a type-level function: T => DD
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from T to DD: T uniquely
   * determines DD. This is, indeed, a function as there is one
   * implicit for any Scala/Java type, but it is not injective
   * (Long and Int map to DLong, for example) so it does not have
   * an inverse function (its inverse is a relation)
   */

  implicit def DDWriterCore2DDWriter[DD <: DatomicData, T]
                                    (implicit ddwc: DDWriterCore[DD, T])
                                    : DDWriter[DD, T] = DDWriter[DD, T](ddwc.write(_))

  implicit val Int2DLong        = DDWriter[DLong,    Int]         ((l: Int)         => DLong(l))
  implicit val Short2DLong      = DDWriter[DLong,    Short]       ((s: Short)       => DLong(s))
  implicit val Char2DLong       = DDWriter[DLong,    Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DLong       = DDWriter[DLong,    Byte]        ((b: Byte)        => DLong(b))
  implicit val JBigInt2DBigInt  = DDWriter[DBigInt,  JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DBigDec  = DDWriter[DBigDec,  JBigDecimal] ((i: JBigDecimal) => DBigDec(i))

  implicit val Referenceable2DRef = DDWriter[DRef, Referenceable] ((r: Referenceable) => r.ref)

  implicit def DRef2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DDWriter[DRef, C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DSet2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DatomicDataWriter[A]) =
    DDWriter[DSet, C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }

}
