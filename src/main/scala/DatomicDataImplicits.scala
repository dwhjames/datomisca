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

object DatomicDataImplicits 
  extends DDReaderImplicits
  with    DDWriterImplicits

trait DDReaderImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData]  = DDReader[DatomicData, DD](_.asInstanceOf[DD])
  //implicit def DD2DD[DD <: DatomicData]           = DDReader[DD, DD](_.asInstanceOf[DD])

  implicit val DRef2DRef:               DDReader[DRef, DRef]                        = DDReader( dd => dd )
  implicit val DEntity2DEntity:         DDReader[DEntity, DEntity]                  = DDReader( dd => dd )

  implicit val DatomicData2String:      DDReader[DatomicData, String]               = DDReader(_.asInstanceOf[DString] .underlying)
  implicit val DatomicData2Long:        DDReader[DatomicData, Long]                 = DDReader(_.asInstanceOf[DLong]   .underlying)
  implicit val DatomicData2Boolean:     DDReader[DatomicData, Boolean]              = DDReader(_.asInstanceOf[DBoolean].underlying)
  implicit val DatomicData2Float:       DDReader[DatomicData, Float]                = DDReader(_.asInstanceOf[DFloat]  .underlying)
  implicit val DatomicData2Double:      DDReader[DatomicData, Double]               = DDReader(_.asInstanceOf[DDouble] .underlying)
  implicit val DatomicData2BigInt:      DDReader[DatomicData, BigInt]               = DDReader(_.asInstanceOf[DBigInt] .underlying)
  implicit val DatomicData2BigDec:      DDReader[DatomicData, BigDecimal]           = DDReader(_.asInstanceOf[DBigDec] .underlying)
  implicit val DatomicData2BigIntJava:  DDReader[DatomicData, java.math.BigInteger] = DDReader(_.asInstanceOf[DBigInt] .underlying.underlying)
  implicit val DatomicData2BigDecJava:  DDReader[DatomicData, java.math.BigDecimal] = DDReader(_.asInstanceOf[DBigDec] .underlying.underlying)
  implicit val DatomicData2Date:        DDReader[DatomicData, java.util.Date]       = DDReader(_.asInstanceOf[DInstant].underlying)

  implicit val DString2String:          DDReader[DString, String]                   = DDReader(_.underlying)
  implicit val DLong2Long:              DDReader[DLong, Long]                       = DDReader(_.underlying)
  implicit val DBoolean2Boolean:        DDReader[DBoolean, Boolean]                 = DDReader(_.underlying)
  implicit val DFloat2Float:            DDReader[DFloat, Float]                     = DDReader(_.underlying)
  implicit val DDouble2Double:          DDReader[DDouble, Double]                   = DDReader(_.underlying)
  implicit val DBigInt2BigInt:          DDReader[DBigInt, BigInt]                   = DDReader(_.underlying)
  implicit val DBigDec2BigDec:          DDReader[DBigDec, BigDecimal]               = DDReader(_.underlying)
  implicit val DBigInt2BigIntJava:      DDReader[DBigInt, java.math.BigInteger]     = DDReader(_.underlying.underlying)
  implicit val DBigDec2BigDecJava:      DDReader[DBigDec, java.math.BigDecimal]     = DDReader(_.underlying.underlying)
  implicit val DDInstant2Date:          DDReader[DInstant, java.util.Date]          = DDReader(_.underlying)

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] =
    DDReader(_.asInstanceOf[DSet].toSet.map( reader.read(_) ))
}

trait DDWriterImplicits{

  implicit val String2DStringWrites     = DDWriter[DString, String]              ( (s: String)                    => DString(s) )
  implicit val Long2DLongWrites         = DDWriter[DLong, Long]                  ( (l: Long)                      => DLong(l) )
  implicit val Int2DIntWrites           = DDWriter[DInt, Int]                    ( (l: Int)                       => DInt(l) )
  implicit val Boolean2DBooleanWrites   = DDWriter[DBoolean, Boolean]            ( (b: Boolean)                   => DBoolean(b) )
  implicit val Float2DFloatWrites       = DDWriter[DFloat, Float]                ( (b: Float)                     => DFloat(b) )
  implicit val Double2DDoubleWrites     = DDWriter[DDouble, Double]              ( (b: Double)                    => DDouble(b) )
  implicit val Date2DDateWrites         = DDWriter[DInstant, java.util.Date]     ( (d: java.util.Date)            => DInstant(d) )
  implicit val JavaBigInt2DBigIntWrites = DDWriter[DBigInt, java.math.BigInteger]( (i: java.math.BigInteger)      => DBigInt(i) )
  implicit val JavaBigDec2DBigDecWrites = DDWriter[DBigDec, java.math.BigDecimal]( (i: java.math.BigDecimal)      => DBigDec(i) )
  implicit val BigInt2DBigIntWrites     = DDWriter[DBigInt, BigInt]              ( (i: BigInt)                    => DBigInt(i) )
  implicit val BigDec2DBigDecWrites     = DDWriter[DBigDec, BigDecimal]          ( (i: BigDecimal)                => DBigDec(i) )
  implicit val Ref2DReferenceable       = DDWriter[DRef, Referenceable]          ( (referenceable: Referenceable) => referenceable.ref )
  implicit val DRef2DRefWrites          = DDWriter[DRef, DRef]                   ( (d: DRef) => d )

  implicit val DD2StringWrites          = DDWriter[DatomicData, String]                 ( (s: String)                    => DString(s) )
  implicit val DD2LongWrites            = DDWriter[DatomicData, Long]                   ( (l: Long)                      => DLong(l) )
  implicit val DD2IntWrites             = DDWriter[DatomicData, Int]                    ( (l: Int)                       => DInt(l) )
  implicit val DD2BooleanWrites         = DDWriter[DatomicData, Boolean]                ( (b: Boolean)                   => DBoolean(b) )
  implicit val DD2FloatWrites           = DDWriter[DatomicData, Float]                  ( (b: Float)                     => DFloat(b) )
  implicit val DD2DoubleWrites          = DDWriter[DatomicData, Double]                 ( (b: Double)                    => DDouble(b) )
  implicit val DD2DateWrites            = DDWriter[DatomicData, java.util.Date]         ( (d: java.util.Date)            => DInstant(d) )
  implicit val DD2JavaBigIntWrites      = DDWriter[DatomicData, java.math.BigInteger]   ( (i: java.math.BigInteger)      => DBigInt(i) )
  implicit val DD2JavaBigDecWrites      = DDWriter[DatomicData, java.math.BigDecimal]   ( (i: java.math.BigDecimal)      => DBigDec(i) )
  implicit val DD2BigIntWrites          = DDWriter[DatomicData, BigInt]                 ( (i: BigInt)                    => DBigInt(i) )
  implicit val DD2BigDecWrites          = DDWriter[DatomicData, BigDecimal]             ( (i: BigDecimal)                => DBigDec(i) )
  implicit val DD2Referenceable         = DDWriter[DatomicData, Referenceable]          ( (referenceable: Referenceable) => referenceable.ref )
  //implicit val DD2DRefWrites             = DDWriter[DatomicData, DRef]                   ( (d: DRef) => d )

  implicit def DDatomicData[DD <: DatomicData] = DDWriter[DatomicData, DD]( dd => dd.asInstanceOf[DD] )

  /*implicit def DD2DStringWrites         = DDWriter[DatomicData, DString] (_.asInstanceOf[DString])
  implicit def DD2DLongWrites           = DDWriter[DatomicData, DLong]   (_.asInstanceOf[DLong])
  implicit def DD2DBooleanWrites        = DDWriter[DatomicData, DBoolean](_.asInstanceOf[DBoolean])
  implicit def DD2DFloatWrites          = DDWriter[DatomicData, DFloat]  (_.asInstanceOf[DFloat])
  implicit def DD2DDoubleWrites         = DDWriter[DatomicData, DDouble] (_.asInstanceOf[DDouble])
  implicit def DD2DInstantWrite         = DDWriter[DatomicData, DInstant](_.asInstanceOf[DInstant])
  implicit def DD2DBigIntWrites         = DDWriter[DatomicData, DBigInt] (_.asInstanceOf[DBigInt])
  implicit def DD2DBigDecWrites         = DDWriter[DatomicData, DBigDec] (_.asInstanceOf[DBigDec])
  implicit def DD2DRefWrites            = DDWriter[DatomicData, DRef]    (_.asInstanceOf[DRef])
  implicit def DD2DSetWrites            = DDWriter[DatomicData, DSet]    (_.asInstanceOf[DSet])
  implicit def DD2TempIdWrites          = DDWriter[DatomicData, TempId]  (_.asInstanceOf[TempId])
  implicit def DD2FinalIdWrites         = DDWriter[DatomicData, FinalId] (_.asInstanceOf[FinalId])
  implicit def DD2DEntityWrites         = DDWriter[DatomicData, DEntity] (_.asInstanceOf[DEntity])*/
  
  implicit def DD2RefWrites             = DDWriter[DatomicData, Ref[_]]( (ref: Ref[_]) => DRef(ref.id) )
  implicit def DRef2RefWrites           = DDWriter[DRef, Ref[_]]( (ref: Ref[_]) => DRef(ref.id) )

  implicit def DD2SetWrites[A](implicit ddw: DDWriter[DatomicData, A]) =
    DDWriter[DatomicData, Traversable[A]]{ (l: Traversable[A]) => DSet(l.map{ a => Datomic.toDatomic(a)(ddw) }.toSet) }

  implicit def DSet2SetWrites[A](implicit ddw: DDWriter[DatomicData, A]) =
    DDWriter[DSet, Traversable[A]]{ (l: Traversable[A]) => DSet(l.map{ a => Datomic.toDatomic(a)(ddw) }.toSet) }

}
