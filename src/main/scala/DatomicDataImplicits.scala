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
  extends DDReaderMultiImplicits
  with    DDReaderMonoImplicits
  with    DDWriterMultiImplicits
  with    DDWriterMonoImplicits
  with    DDWriterEpiImplicits

trait DDReaderMultiImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData] = DDReaderMulti[DD](_.asInstanceOf[DD])

  implicit val DatomicData2String:  DDReaderMulti[String]      = DDReaderMulti(_.asInstanceOf[DString] .underlying)
  implicit val DatomicData2Boolean: DDReaderMulti[Boolean]     = DDReaderMulti(_.asInstanceOf[DBoolean].underlying)
  implicit val DatomicData2Long:    DDReaderMulti[Long]        = DDReaderMulti(_.asInstanceOf[DLong]   .underlying)
  implicit val DatomicData2Float:   DDReaderMulti[Float]       = DDReaderMulti(_.asInstanceOf[DFloat]  .underlying)
  implicit val DatomicData2Double:  DDReaderMulti[Double]      = DDReaderMulti(_.asInstanceOf[DDouble] .underlying)
  implicit val DatomicData2BigInt:  DDReaderMulti[BigInt]      = DDReaderMulti(_.asInstanceOf[DBigInt] .underlying)
  implicit val DatomicData2BigDec:  DDReaderMulti[BigDecimal]  = DDReaderMulti(_.asInstanceOf[DBigDec] .underlying)
  implicit val DatomicData2Date:    DDReaderMulti[Date]        = DDReaderMulti(_.asInstanceOf[DInstant].underlying)
  implicit val DatomicData2UUID:    DDReaderMulti[UUID]        = DDReaderMulti(_.asInstanceOf[DUuid]   .underlying)
  implicit val DatomicData2URI:     DDReaderMulti[URI]         = DDReaderMulti(_.asInstanceOf[DUri]    .underlying)
  implicit val DatomicData2Bytes:   DDReaderMulti[Array[Byte]] = DDReaderMulti(_.asInstanceOf[DBytes]  .underlying)

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReaderMulti[T]): DDReaderMulti[Set[T]] =
    DDReaderMulti(_.asInstanceOf[DSet].toSet.map( reader.read(_) ))
}

trait DDReaderMonoImplicits {

  /*
   * Think of DDReaderMono[DD, T] as a type-level function: DD => T
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from DD to T: DD uniquely
   * determines T. In fact, this is an injective function, as there
   * is at most one DDReaderMono for each DatomicData subtype, and each
   * map to distinct Scala/Java types. As a consequence, its inverse
   * is a partial function.
   */

  implicit val ReadDRef:                DDReaderMono[DRef,     DRef]        = DDReaderMono( dd => dd )
  //implicit val ReadDEntity: DDReaderMono[DEntity, DEntity] = DDReaderMono( dd => dd )

  implicit val DString2String:          DDReaderMono[DString,  String]      = DDReaderMono(_.underlying)
  implicit val DBoolean2Boolean:        DDReaderMono[DBoolean, Boolean]     = DDReaderMono(_.underlying)
  implicit val DLong2Long:              DDReaderMono[DLong,    Long]        = DDReaderMono(_.underlying)
  implicit val DFloat2Float:            DDReaderMono[DFloat,   Float]       = DDReaderMono(_.underlying)
  implicit val DDouble2Double:          DDReaderMono[DDouble,  Double]      = DDReaderMono(_.underlying)
  implicit val DBigInt2BigInt:          DDReaderMono[DBigInt,  BigInt]      = DDReaderMono(_.underlying)
  implicit val DBigDec2BigDec:          DDReaderMono[DBigDec,  BigDecimal]  = DDReaderMono(_.underlying)
  implicit val DInstant2Date:           DDReaderMono[DInstant, Date]        = DDReaderMono(_.underlying)
  implicit val DUuid2UUID:              DDReaderMono[DUuid,    UUID]        = DDReaderMono(_.underlying)
  implicit val DUri2URI:                DDReaderMono[DUri,     URI]         = DDReaderMono(_.underlying)
  implicit val DBytes2Bytes:            DDReaderMono[DBytes,   Array[Byte]] = DDReaderMono(_.underlying)

}

trait DDWriterMultiImplicits {
  implicit def DDWriter2DDWriterMulti[DD <: DatomicData, A](implicit ddw: DDWriterEpi[DD, A]) = 
    DDWriterMulti[A] { (a: A) => ddw.write(a): DatomicData }

  /*implicit val String2DatomicData  = DDWriterMulti[String]      ((s: String)      => DString(s))
  implicit val Boolean2DatomicData = DDWriterMulti[Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DatomicData    = DDWriterMulti[Long]        ((l: Long)        => DLong(l))
  implicit val Int2DatomicData     = DDWriterMulti[Int]         ((i: Int)         => DLong(i))
  implicit val Short2DatomicData   = DDWriterMulti[Short]       ((s: Short)       => DLong(s))
  implicit val Char2DatomicData    = DDWriterMulti[Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DatomicData    = DDWriterMulti[Byte]        ((b: Byte)        => DLong(b))
  implicit val Float2DatomicData   = DDWriterMulti[Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DatomicData  = DDWriterMulti[Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DatomicData  = DDWriterMulti[BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DatomicData  = DDWriterMulti[BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val JBigInt2DatomicData = DDWriterMulti[JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DatomicData = DDWriterMulti[JBigDecimal] ((i: JBigDecimal) => DBigDec(i))
  implicit val Date2DatomicData    = DDWriterMulti[Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DatomicData    = DDWriterMulti[UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DatomicData     = DDWriterMulti[URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DatomicData   = DDWriterMulti[Array[Byte]] ((a: Array[Byte]) => DBytes(a))

  implicit def Referenceable2DatomicData[A <: Referenceable] = DDWriterMulti[A]{ (a: A) => a.ref }
  //implicit val DRef2DatomicData          = DDWriterMulti[DRef]          ((d: DRef) => d)

  implicit def DDatomicData[DD <: DatomicData] = DDWriterMulti[DD]( dd => dd.asInstanceOf[DD] )

  implicit def DD2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DDWriterMulti[C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DD2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DDWriterMulti[A]) =
    DDWriterMulti[C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }
  */
}

trait DDWriterMonoImplicits {

  /*
   * Think of DDWriterMono[DD, T] as a type-level function: T => DD
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from T to DD: T uniquely
   * determines DD.  In fact, this is an injective function, as there
   * is at most one DDWriterMono for any Scala type, and each
   * map to distinct DatomicData subtypes. As a consequence, its inverse
   * is a partial function.
   */

  implicit val String2DString   = DDWriterMono[DString,  String]      ((s: String)      => DString(s))
  implicit val Boolean2DBoolean = DDWriterMono[DBoolean, Boolean]     ((b: Boolean)     => DBoolean(b))
  implicit val Long2DLong       = DDWriterMono[DLong,    Long]        ((l: Long)        => DLong(l))
  implicit val Float2DFloat     = DDWriterMono[DFloat,   Float]       ((b: Float)       => DFloat(b))
  implicit val Double2DDouble   = DDWriterMono[DDouble,  Double]      ((b: Double)      => DDouble(b))
  implicit val BigInt2DBigInt   = DDWriterMono[DBigInt,  BigInt]      ((i: BigInt)      => DBigInt(i))
  implicit val BigDec2DBigDec   = DDWriterMono[DBigDec,  BigDecimal]  ((i: BigDecimal)  => DBigDec(i))
  implicit val Date2DDate       = DDWriterMono[DInstant, Date]        ((d: Date)        => DInstant(d))
  implicit val UUID2DUuid       = DDWriterMono[DUuid,    UUID]        ((u: UUID)        => DUuid(u))
  implicit val URI2DUri         = DDWriterMono[DUri,     URI]         ((u: URI)         => DUri(u))
  implicit val Bytes2DBytes     = DDWriterMono[DBytes,   Array[Byte]] ((a: Array[Byte]) => DBytes(a))

  implicit val WriteDRef        = DDWriterMono[DRef,     DRef]        ((d: DRef)        => d)

}

trait DDWriterEpiImplicits {

  /*
   * Think of DDWriterEpi[DD, T] as a type-level function: T => DD
   * The implicits here construct a multi-parameter type class,
   * and there is a functional dependency from T to DD: T uniquely
   * determines DD. This is, indeed, a function as there is one
   * implicit for any Scala/Java type, but it is not injective
   * (Long and Int map to DLong, for example) so it does not have
   * an inverse function (its inverse is a relation)
   */

  implicit def DDWriterMono2DDWriterEpi[DD <: DatomicData, T]
                                    (implicit ddwc: DDWriterMono[DD, T])
                                    : DDWriterEpi[DD, T] = DDWriterEpi[DD, T](ddwc.write(_))

  implicit val Int2DLong        = DDWriterEpi[DLong,    Int]         ((l: Int)         => DLong(l))
  implicit val Short2DLong      = DDWriterEpi[DLong,    Short]       ((s: Short)       => DLong(s))
  implicit val Char2DLong       = DDWriterEpi[DLong,    Char]        ((c: Char)        => DLong(c))
  implicit val Byte2DLong       = DDWriterEpi[DLong,    Byte]        ((b: Byte)        => DLong(b))
  implicit val JBigInt2DBigInt  = DDWriterEpi[DBigInt,  JBigInt]     ((i: JBigInt)     => DBigInt(i))
  implicit val JBigDec2DBigDec  = DDWriterEpi[DBigDec,  JBigDecimal] ((i: JBigDecimal) => DBigDec(i))

  //implicit val Referenceable2DRef = DDWriterEpi[DRef, Referenceable] ((r: Referenceable) => r.ref)
  implicit def Referenceable2DRef[A <: Referenceable] = DDWriterEpi[DRef, A]{ (a: A) => a.ref }

  implicit def DDatomicData[DD <: DatomicData] = DDWriterEpi[DD, DD]( dd => dd.asInstanceOf[DD] )

  implicit def DRef2RefWrites[C, A](implicit witness: C <:< IdView[A]) =
    DDWriterEpi[DRef, C]{ (ref: C) => DRef(witness(ref).id) }

  implicit def DSet2SetWrites[C, A](implicit witness: C <:< Traversable[A], ddw: DDWriterMulti[A]) =
    DDWriterEpi[DSet, C]{ (l: C) => 
      DSet(witness(l).map{ (a: A) => Datomic.toDatomic(a)(ddw) }.toSet) 
    }

}
