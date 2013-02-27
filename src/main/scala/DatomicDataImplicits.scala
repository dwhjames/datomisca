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
  extends DD2ScalaReaderImplicits 
  with DD2DDReaderImplicits
  with DDReaderImplicits
  with DDWriterImplicits


trait DD2ScalaReaderImplicits {
  implicit val DString2String     = DD2ScalaReader{ s: DString  => s.underlying }
  implicit val DLong2Long         = DD2ScalaReader{ s: DLong    => s.underlying }
  implicit val DBoolean2Boolean   = DD2ScalaReader{ s: DBoolean => s.underlying }
  implicit val DFloat2Float       = DD2ScalaReader{ s: DFloat   => s.underlying }
  implicit val DDouble2Double     = DD2ScalaReader{ s: DDouble  => s.underlying }
  implicit val DBigInt2BigInteger = DD2ScalaReader{ s: DBigInt  => s.underlying }
  implicit val DBigDec2BigDecimal = DD2ScalaReader{ s: DBigDec  => s.underlying }
  implicit val DInstant2Date      = DD2ScalaReader{ s: DInstant => s.underlying }

  implicit val DRef2DRef = DD2ScalaReader{ s: DRef => s }
  implicit val DEntity2DEntity = DD2ScalaReader{ s: DEntity => s }

  //implicit val DString2DString = DD2ScalaReader{ s: DString => s }
  //implicit val DInstant2DInstant = DD2ScalaReader{ s: DInstant => s }
  //implicit def DD2DD[DD <: DatomicData] = DD2ScalaReader{ d: DD => d: DD }
}

trait DD2DDReaderImplicits {
  
  implicit def DSet2T[DD <: DatomicData, T]
    (implicit dd2dd: DD2DDReader[DD], dd2t: DD2ScalaReader[DD, T]): DD2ScalaReader[DSet, Set[T]] = {
    DD2ScalaReader{ ds: DSet => ds.toSet.map( e => dd2t.read(dd2dd.read(e)) ) }
  }

  implicit val DatomicData2DString  = DD2DDReader(_.asInstanceOf[DString])
  implicit val DatomicData2DLong    = DD2DDReader(_.asInstanceOf[DLong])
  implicit val DatomicData2DBoolean = DD2DDReader(_.asInstanceOf[DBoolean])
  implicit val DatomicData2DFloat   = DD2DDReader(_.asInstanceOf[DFloat])
  implicit val DatomicData2DDouble  = DD2DDReader(_.asInstanceOf[DDouble])
  implicit val DatomicData2DBigInt  = DD2DDReader(_.asInstanceOf[DBigInt])
  implicit val DatomicData2DBigDec  = DD2DDReader(_.asInstanceOf[DBigDec])
  implicit val DatomicData2DInstant = DD2DDReader(_.asInstanceOf[DInstant])
  implicit val DatomicData2DEntity  = DD2DDReader(_.asInstanceOf[DEntity])
  implicit val DatomicData2DSet     = DD2DDReader(_.asInstanceOf[DSet])
  implicit val DatomicData2DRef     = DD2DDReader(_.asInstanceOf[DRef])
}

trait DDReaderImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData](implicit dd2dd: DD2DDReader[DD]): DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd2dd.read(dd) }
  /*implicit def genericDDReader[A](implicit dd2t: DD2ScalaReader[DatomicData, A]): DDReader[DatomicData, A] = 
    DDReader{ dd: DatomicData =>
      dd2t.read(dd)
    }*/

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

  /*implicit val DRefDDReader: DDReader[DatomicData, DRef] = DDReader{ dd: DatomicData => dd match {
    case s: DRef => s
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit val DEntityDDReader: DDReader[DatomicData, DEntity] = DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}*/

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] =
    DDReader(_.asInstanceOf[DSet].toSet.map( reader.read(_) ))
}

trait DDWriterImplicits{

  /*implicit def DatomicData2DD[DD <: DatomicData]: DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd match { 
    case s: DD => s
    case _ => throw new RuntimeException("couldn't convert")
  }}*/

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
  //implicit def DDatomicData[DD <: DatomicData] = DDWriter[DD, DD]( dd => dd )

  implicit def DD2DStringWrites  = DDWriter[DatomicData, DString] (_.asInstanceOf[DString])
  implicit def DD2DLongWrites    = DDWriter[DatomicData, DLong]   (_.asInstanceOf[DLong])
  implicit def DD2DBooleanWrites = DDWriter[DatomicData, DBoolean](_.asInstanceOf[DBoolean])
  implicit def DD2DFloatWrites   = DDWriter[DatomicData, DFloat]  (_.asInstanceOf[DFloat])
  implicit def DD2DDoubleWrites  = DDWriter[DatomicData, DDouble] (_.asInstanceOf[DDouble])
  implicit def DD2DInstantWrites = DDWriter[DatomicData, DInstant](_.asInstanceOf[DInstant])
  implicit def DD2DBigIntWrites  = DDWriter[DatomicData, DBigInt] (_.asInstanceOf[DBigInt])
  implicit def DD2DBigDecWrites  = DDWriter[DatomicData, DBigDec] (_.asInstanceOf[DBigDec])
  implicit def DD2DRefWrites     = DDWriter[DatomicData, DRef]    (_.asInstanceOf[DRef])
  implicit def DD2DSetWrites     = DDWriter[DatomicData, DSet]    (_.asInstanceOf[DSet])
  implicit def DD2TempIdWrites   = DDWriter[DatomicData, TempId]  (_.asInstanceOf[TempId])
  implicit def DD2FinalIdWrites  = DDWriter[DatomicData, FinalId] (_.asInstanceOf[FinalId])
  implicit def DD2DEntityWrites  = DDWriter[DatomicData, DEntity] (_.asInstanceOf[DEntity])
  implicit def DRefWrites        = DDWriter[DRef, Ref[_]]( (ref: Ref[_]) => DRef(ref.id) )

  implicit def DSetWrites[A](implicit ddw: DDWriter[DatomicData, A]) =
    DDWriter[DSet, Traversable[A]]{ (l: Traversable[A]) => DSet(l.map{ a => Datomic.toDatomic(a)(ddw) }.toSet) }

  //implicit def ddIdentity = DDWriter[DatomicData, DatomicData]{ dd => dd }

}

trait DD2WriterImplicits {

  implicit def ddIdentity = DD2Writer[DatomicData]{ dd => dd }

  implicit val StringDD2Writes        = DD2Writer[String]              ( (s: String)                    => DString(s) )
  implicit val LongDD2Writes          = DD2Writer[Long]                ( (l: Long)                      => DLong(l) )
  implicit val IntDD2Writes           = DD2Writer[Int]                 ( (l: Int)                       => DInt(l) )
  implicit val BooleanDD2Writes       = DD2Writer[Boolean]             ( (b: Boolean)                   => DBoolean(b) )
  implicit val FloatDD2Writes         = DD2Writer[Float]               ( (b: Float)                     => DFloat(b) )
  implicit val DoubleDD2Writes        = DD2Writer[Double]              ( (b: Double)                    => DDouble(b) )
  implicit val DateDD2Writes          = DD2Writer[java.util.Date]      ( (d: java.util.Date)            => DInstant(d) )
  implicit val JavaBigIntDD2Writes    = DD2Writer[java.math.BigInteger]( (i: java.math.BigInteger)      => DBigInt(i) )
  implicit val JavaBigDecDD2Writes    = DD2Writer[java.math.BigDecimal]( (i: java.math.BigDecimal)      => DBigDec(i) )
  implicit val BigIntDD2Writes        = DD2Writer[BigInt]              ( (i: BigInt)                    => DBigInt(i) )
  implicit val BigDecDD2Writes        = DD2Writer[BigDecimal]          ( (i: BigDecimal)                => DBigDec(i) )
  implicit val ReferenceableDD2Writes = DD2Writer[Referenceable]       ( (referenceable: Referenceable) => referenceable.ref )

}
