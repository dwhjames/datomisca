package reactivedatomic

object DatomicDataImplicits 
  extends DD2ScalaReaderImplicits 
  with DD2DDReaderImplicits
  with DDReaderImplicits
  with DDWriterImplicits


trait DD2ScalaReaderImplicits {
  implicit val DString2String = DD2ScalaReader{ s: DString => s.underlying }
  implicit val DLong2Long = DD2ScalaReader{ s: DLong => s.underlying }
  implicit val DBoolean2Boolean = DD2ScalaReader{ s: DBoolean => s.underlying }
  implicit val DFloat2Float = DD2ScalaReader{ s: DFloat => s.underlying }
  implicit val DDouble2Double = DD2ScalaReader{ s: DDouble => s.underlying }
  implicit val DBigInt2BigInteger = DD2ScalaReader{ s: DBigInt => s.underlying }
  implicit val DBigDec2BigDecimal = DD2ScalaReader{ s: DBigDec => s.underlying }
  implicit val DInstant2Date = DD2ScalaReader{ s: DInstant => s.underlying }

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

  implicit val DatomicData2DString: DD2DDReader[DString] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DString => s
    case _ => throw new RuntimeException("expected DString to convert to String")
  }}
  
  implicit val DatomicData2DLong: DD2DDReader[DLong] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s
    case _ => throw new RuntimeException("expected DLong to convert to DLong")
  }}

  implicit val DatomicData2DBoolean: DD2DDReader[DBoolean] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBoolean => s
    case _ => throw new RuntimeException("expected DBoolean to convert to DBoolean")
  }}

  implicit val DatomicData2DFloat: DD2DDReader[DFloat] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DFloat => s
    case _ => throw new RuntimeException("expected DFloat to convert to DFloat")
  }}

  implicit val DatomicData2DDouble: DD2DDReader[DDouble] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DDouble => s
    case _ => throw new RuntimeException("expected DDouble to convert to DDouble")
  }}

  implicit val DatomicData2DBigInt: DD2DDReader[DBigInt] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBigInt => s
    case _ => throw new RuntimeException("expected DBigInt to convert to DBigInt")
  }}

  implicit val DatomicData2DBigDec: DD2DDReader[DBigDec] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DBigDec => s
    case _ => throw new RuntimeException("expected DBigDec to convert to DBigDec")
  }}

  implicit val DatomicData2DInstant: DD2DDReader[DInstant] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s
    case _ => throw new RuntimeException("expected DInstant to convert to DInstant")
  }}

  implicit val DatomicData2DEntity: DD2DDReader[DEntity] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}

  implicit val DatomicData2DSet: DD2DDReader[DSet] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  implicit val DatomicData2DRef: DD2DDReader[DRef] = DD2DDReader{ dd: DatomicData => dd match { 
    case s: DRef => s
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}
}

trait DDReaderImplicits {
  implicit def Datomicdata2DD[DD <: DatomicData](implicit dd2dd: DD2DDReader[DD]): DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd2dd.read(dd) }
  /*implicit def genericDDReader[A](implicit dd2t: DD2ScalaReader[DatomicData, A]): DDReader[DatomicData, A] = 
    DDReader{ dd: DatomicData =>
      dd2t.read(dd)
    }*/

  implicit val DatomicData2String: DDReader[DatomicData, String] = DDReader{ dd: DatomicData => dd match { 
    case s: DString => s.underlying 
    case _ => throw new RuntimeException("expected DString to convert to String")
  }}

  implicit val DatomicData2Long: DDReader[DatomicData, Long] = DDReader{ dd: DatomicData => dd match { 
    case s: DLong => s.underlying 
    case _ => throw new RuntimeException("expected DLong to convert to Long")
  }}

  implicit val DatomicData2Boolean: DDReader[DatomicData, Boolean] = DDReader{ dd: DatomicData => dd match { 
    case s: DBoolean => s.underlying 
    case _ => throw new RuntimeException("expected DBoolean to convert to Boolean")
  }}

  implicit val DatomicData2Float: DDReader[DatomicData, Float] = DDReader{ dd: DatomicData => dd match { 
    case s: DFloat => s.underlying 
    case _ => throw new RuntimeException("expected DFloat to convert to Float")
  }}

  implicit val DatomicData2Double: DDReader[DatomicData, Double] = DDReader{ dd: DatomicData => dd match { 
    case s: DDouble => s.underlying 
    case _ => throw new RuntimeException("expected DDouble to convert to Double")
  }}

  implicit val DatomicData2BigInt: DDReader[DatomicData, BigInt] = DDReader{ dd: DatomicData => dd match { 
    case s: DBigInt => s.underlying 
    case _ => throw new RuntimeException("expected DBigInt to convert to BigInteger")
  }}

  implicit val DatomicData2BigDec: DDReader[DatomicData, BigDecimal] = DDReader{ dd: DatomicData => dd match { 
    case s: DBigDec => s.underlying 
    case _ => throw new RuntimeException("expected DBigDec to convert to BigDecimal")
  }}

  implicit val DatomicData2Date: DDReader[DatomicData, java.util.Date] = DDReader{ dd: DatomicData => dd match { 
    case s: DInstant => s.underlying 
    case _ => throw new RuntimeException("expected DInstant to convert to Data")
  }}

  /*implicit val DRefDDReader: DDReader[DatomicData, DRef] = DDReader{ dd: DatomicData => dd match { 
    case s: DRef => s
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit val DEntityDDReader: DDReader[DatomicData, DEntity] = DDReader{ dd: DatomicData => dd match { 
    case s: DEntity => s
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}*/

  implicit def DatomicData2DSetTyped[T](implicit reader: DDReader[DatomicData, T]): DDReader[DatomicData, Set[T]] = DDReader{ dd: DatomicData => dd match { 
    case s: DSet => s.toSet.map( reader.read(_) )
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}
}

trait DDWriterImplicits{

  /*implicit def DatomicData2DD[DD <: DatomicData]: DDReader[DatomicData, DD] = DDReader{ dd: DatomicData => dd match { 
    case s: DD => s
    case _ => throw new RuntimeException("couldn't convert")
  }}*/

  implicit val String2DStringWrites = DDWriter[DString, String]( (s: String) => DString(s) )
  implicit val Long2DLongWrites = DDWriter[DLong, Long]( (l: Long) => DLong(l) )
  implicit val Int2DIntWrites = DDWriter[DInt, Int]( (l: Int) => DInt(l) )
  implicit val Boolean2DBooleanWrites = DDWriter[DBoolean, Boolean]( (b: Boolean) => DBoolean(b) )
  implicit val Float2DFloatWrites = DDWriter[DFloat, Float]( (b: Float) => DFloat(b) )
  implicit val Double2DDoubleWrites = DDWriter[DDouble, Double]( (b: Double) => DDouble(b) )
  implicit val Date2DDateWrites = DDWriter[DInstant, java.util.Date]( (d: java.util.Date) => DInstant(d) )
  implicit val BigInt2DBigIntWrites = DDWriter[DBigInt, java.math.BigInteger]( (i: java.math.BigInteger) => DBigInt(i) )
  implicit val BigDec2DBigDecWrites = DDWriter[DBigDec, java.math.BigDecimal]( (i: java.math.BigDecimal) => DBigDec(i) )
  implicit val Ref2DReferenceable = DDWriter[DRef, Referenceable]( (referenceable: Referenceable) => referenceable.ident )
  implicit val DRef2DRefWrites = DDWriter[DRef, DRef]( (d: DRef) => d )
  //implicit def DDatomicData[DD <: DatomicData] = DDWriter[DD, DD]( dd => dd )
  
  implicit def DD2DStringWrites = DDWriter[DatomicData, DString]{ dd: DatomicData => dd match {
    case d: DString => d
    case _ => throw new RuntimeException("expected DString to convert to DString")
  }}

  implicit def DD2DLongWrites = DDWriter[DatomicData, DLong]{ dd: DatomicData => dd match {
    case d: DLong => d
    case _ => throw new RuntimeException("expected DLong to convert to DLong")
  }}

  implicit def DD2DBooleanWrites = DDWriter[DatomicData, DBoolean]{ dd: DatomicData => dd match {
    case d: DBoolean => d
    case _ => throw new RuntimeException("expected DBoolean to convert to DBoolean")
  }}

  implicit def DD2DFloatWrites = DDWriter[DatomicData, DFloat]{ dd: DatomicData => dd match {
    case d: DFloat => d
    case _ => throw new RuntimeException("expected DFloat to convert to DFloat")
  }}

  implicit def DD2DDoubleWrites = DDWriter[DatomicData, DDouble]{ dd: DatomicData => dd match {
    case d: DDouble => d
    case _ => throw new RuntimeException("expected DDouble to convert to DDouble")
  }}

  implicit def DD2DInstantWrites = DDWriter[DatomicData, DInstant]{ dd: DatomicData => dd match {
    case d: DInstant => d
    case _ => throw new RuntimeException("expected DInstant to convert to DInstant")
  }}

  implicit def DD2DBigIntWrites = DDWriter[DatomicData, DBigInt]{ dd: DatomicData => dd match {
    case d: DBigInt => d
    case _ => throw new RuntimeException("expected DBigInt to convert to DBigInt")
  }}

  implicit def DD2DBigDecWrites = DDWriter[DatomicData, DBigDec]{ dd: DatomicData => dd match {
    case d: DBigDec => d
    case _ => throw new RuntimeException("expected DBigDec to convert to DBigDec")
  }}

  implicit def DD2DRefWrites = DDWriter[DatomicData, DRef]{ dd: DatomicData => dd match {
    case d: DRef => d
    case _ => throw new RuntimeException("expected DRef to convert to DRef")
  }}

  implicit def DD2DSetWrites = DDWriter[DatomicData, DSet]{ dd: DatomicData => dd match {
    case d: DSet => d
    case _ => throw new RuntimeException("expected DSet to convert to DSet")
  }}

  implicit def DD2TempIdWrites = DDWriter[DatomicData, TempId]{ dd: DatomicData => dd match {
    case d: TempId => d
    case _ => throw new RuntimeException("expected TempId to convert to TempId")
  }}

  implicit def DD2FinalIdWrites = DDWriter[DatomicData, FinalId]{ dd: DatomicData => dd match {
    case d: FinalId => d
    case _ => throw new RuntimeException("expected FinalId to convert to FinalId")
  }}

  implicit def DD2DEntityWrites = DDWriter[DatomicData, DEntity]{ dd: DatomicData => dd match {
    case d: DEntity => d
    case _ => throw new RuntimeException("expected DEntity to convert to DEntity")
  }}

  implicit def DRefWrites = DDWriter[DRef, Ref[_]]( (ref: Ref[_]) => DRef(ref.id) )

  implicit def DSetWrites[A](implicit ddw: DDWriter[DatomicData, A]) = 
    DDWriter[DSet, Traversable[A]]{ (l: Traversable[A]) => DSet(l.map{ a => Datomic.toDatomic(a)(ddw) }.toSet) }

  //implicit def ddIdentity = DDWriter[DatomicData, DatomicData]{ dd => dd }

}

trait DD2WriterImplicits {

  implicit def ddIdentity = DD2Writer[DatomicData]{ dd => dd }

  implicit val StringDD2Writes = DD2Writer[String]( (s: String) => DString(s) )
  implicit val LongDD2Writes = DD2Writer[Long]( (l: Long) => DLong(l) )
  implicit val IntDD2Writes = DD2Writer[Int]( (l: Int) => DInt(l) )
  implicit val BooleanDD2Writes = DD2Writer[Boolean]( (b: Boolean) => DBoolean(b) )
  implicit val FloatDD2Writes = DD2Writer[Float]( (b: Float) => DFloat(b) )
  implicit val DoubleDD2Writes = DD2Writer[Double]( (b: Double) => DDouble(b) )
  implicit val DateDD2Writes = DD2Writer[java.util.Date]( (d: java.util.Date) => DInstant(d) )
  implicit val BigIntDD2Writes = DD2Writer[java.math.BigInteger]( (i: java.math.BigInteger) => DBigInt(i) )
  implicit val BigDecDD2Writes = DD2Writer[java.math.BigDecimal]( (i: java.math.BigDecimal) => DBigDec(i) )
  implicit val ReferenceableDD2Writes = DD2Writer[Referenceable]( (referenceable: Referenceable) => referenceable.ident )

}
