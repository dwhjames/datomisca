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
  extends DDReaderImplicits
  with    DDWriterImplicits

trait DDReaderImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData]  = DDReader[DatomicData, DD](_.asInstanceOf[DD])

  implicit val ReadDRef:    DDReader[DRef,    DRef]    = DDReader( dd => dd )
  //implicit val ReadDEntity: DDReader[DEntity, DEntity] = DDReader( dd => dd )

  implicit val DatomicData2String:      DDReader[DatomicData, String]      = DDReader(_.asInstanceOf[DString] .underlying)
  implicit val DatomicData2Boolean:     DDReader[DatomicData, Boolean]     = DDReader(_.asInstanceOf[DBoolean].underlying)
  implicit val DatomicData2Long:        DDReader[DatomicData, Long]        = DDReader(_.asInstanceOf[DLong]   .underlying)
  implicit val DatomicData2Float:       DDReader[DatomicData, Float]       = DDReader(_.asInstanceOf[DFloat]  .underlying)
  implicit val DatomicData2Double:      DDReader[DatomicData, Double]      = DDReader(_.asInstanceOf[DDouble] .underlying)
  implicit val DatomicData2BigInt:      DDReader[DatomicData, BigInt]      = DDReader(_.asInstanceOf[DBigInt] .underlying)
  implicit val DatomicData2BigDec:      DDReader[DatomicData, BigDecimal]  = DDReader(_.asInstanceOf[DBigDec] .underlying)
  implicit val DatomicData2Date:        DDReader[DatomicData, Date]        = DDReader(_.asInstanceOf[DInstant].underlying)
  implicit val DatomicData2UUID:        DDReader[DatomicData, UUID]        = DDReader(_.asInstanceOf[DUuid]   .underlying)
  implicit val DatomicData2URI:         DDReader[DatomicData, URI]         = DDReader(_.asInstanceOf[DUri]    .underlying)
  implicit val DatomicData2Bytes:       DDReader[DatomicData, Array[Byte]] = DDReader(_.asInstanceOf[DBytes]  .underlying)

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

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] =
    DDReader(_.asInstanceOf[DSet].toSet.map( reader.read(_) ))
}

trait DDWriterImplicits{

  implicit val String2DString   = DDWriter[DString,  String]        ((s: String)      => DString(s))
  implicit val Boolean2DBoolean = DDWriter[DBoolean, Boolean]       ((b: Boolean)     => DBoolean(b))
  implicit val Long2DLong       = DDWriter[DLong,    Long]          ((l: Long)        => DLong(l))
  implicit val Int2DLong        = DDWriter[DLong,    Int]           ((l: Int)         => DLong(l))
  implicit val Short2DLong      = DDWriter[DLong,    Short]         ((s: Short)       => DLong(s))
  implicit val Char2DLong       = DDWriter[DLong,    Char]          ((c: Char)        => DLong(c))
  implicit val Byte2DLong       = DDWriter[DLong,    Byte]          ((b: Byte)        => DLong(b))
  implicit val Float2DFloat     = DDWriter[DFloat,   Float]         ((b: Float)       => DFloat(b))
  implicit val Double2DDouble   = DDWriter[DDouble,  Double]        ((b: Double)      => DDouble(b))
  implicit val JBigInt2DBigInt  = DDWriter[DBigInt,  JBigInt]       ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DBigDec  = DDWriter[DBigDec,  JBigDecimal]   ((i: JBigDecimal) => DBigDec(i))
  implicit val BigInt2DBigInt   = DDWriter[DBigInt,  BigInt]        ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DBigDec   = DDWriter[DBigDec,  BigDecimal]    ((i: BigDecimal)  => DBigDec(i))
  implicit val Date2DDate       = DDWriter[DInstant, Date]          ((d: Date)        => DInstant(d))
  implicit val UUID2DUuid       = DDWriter[DUuid,    UUID]          ((u: UUID)        => DUuid(u))
  implicit val URI2DUri         = DDWriter[DUri,     URI]           ((u: URI)         => DUri(u))
  implicit val Bytes2DBytes     = DDWriter[DBytes,   Array[Byte]]   ((a: Array[Byte]) => DBytes(a))

  implicit val Referenceable2DRef = DDWriter[DRef,     Referenceable] ((r: Referenceable) => r.ref)
  implicit val WriteDRef          = DDWriter[DRef,     DRef]          ((d: DRef)          => d)

  implicit val String2DatomicData  = DDWriter[DatomicData, String]      ((s: String)      => DString(s))
  implicit val Boolean2DatomicData = DDWriter[DatomicData, Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DatomicData    = DDWriter[DatomicData, Long]        ((l: Long)        => DLong(l))
  implicit val Int2DatomicData     = DDWriter[DatomicData, Int]         ((i: Int)         => DLong(i))
  implicit val Short2DatomicData   = DDWriter[DatomicData, Short]       ((s: Short)       => DLong(s))
  implicit val Char2DatomicData    = DDWriter[DatomicData, Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DatomicData    = DDWriter[DatomicData, Byte]        ((b: Byte)        => DLong(b))
  implicit val Float2DatomicData   = DDWriter[DatomicData, Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DatomicData  = DDWriter[DatomicData, Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DatomicData  = DDWriter[DatomicData, BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DatomicData  = DDWriter[DatomicData, BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val JBigInt2DatomicData = DDWriter[DatomicData, JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DatomicData = DDWriter[DatomicData, JBigDecimal] ((i: JBigDecimal) => DBigDec(i))
  implicit val Date2DatomicData    = DDWriter[DatomicData, Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DatomicData    = DDWriter[DatomicData, UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DatomicData     = DDWriter[DatomicData, URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DatomicData   = DDWriter[DatomicData, Array[Byte]] ((a: Array[Byte]) => DBytes(a))

  implicit def Referenceable2DatomicData[A <: Referenceable] = DDWriter[DatomicData, A]{ (a: A) => a.ref }
  //implicit val DRef2DatomicData          = DDWriter[DatomicData, DRef]          ((d: DRef) => d)

  implicit def DDatomicData[DD <: DatomicData] = DDWriter[DatomicData, DD]( dd => dd.asInstanceOf[DD] )
  
  implicit def DD2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DDWriter[DatomicData, C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DRef2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DDWriter[DRef, C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DD2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DDWriter[DatomicData, A]) =
    DDWriter[DatomicData, C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }

  implicit def DSet2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DDWriter[DatomicData, A]) =
    DDWriter[DSet, C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }

}
