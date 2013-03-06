
import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import Datomic.toDatomic

import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI

@RunWith(classOf[JUnitRunner])
class ToFromDatomicSpec extends Specification {

  "FromDatomicCast" can {

    "read DatomicData as itself" in {

      (DString("string").asInstanceOf[DatomicData]).as[DString]
      (DBoolean(true)   .asInstanceOf[DatomicData]).as[DBoolean]
      (DLong(1L)        .asInstanceOf[DatomicData]).as[DLong]
      (DFloat(1.0f)     .asInstanceOf[DatomicData]).as[DFloat]
      (DDouble(1.0)     .asInstanceOf[DatomicData]).as[DDouble]

      (DBigInt(BigInt(1))    .asInstanceOf[DatomicData]).as[DBigInt]
      (DBigDec(BigDecimal(1)).asInstanceOf[DatomicData]).as[DBigDec]

      (DInstant(new Date).asInstanceOf[DatomicData]).as[DInstant]

      (DUuid(UUID.randomUUID()).asInstanceOf[DatomicData]).as[DUuid]

      (DUri(new URI("urn:isbn:096139210x")).asInstanceOf[DatomicData]).as[DUri]

      (DBytes(Array(Byte.MinValue)).asInstanceOf[DatomicData]).as[DBytes]

      success
    }

    "read DatomicData as its underlying Scala type" in {

      (DString("string").asInstanceOf[DatomicData]).as[String]
      (DBoolean(true)   .asInstanceOf[DatomicData]).as[Boolean]
      (DLong(1L)        .asInstanceOf[DatomicData]).as[Long]
      (DFloat(1.0f)     .asInstanceOf[DatomicData]).as[Float]
      (DDouble(1.0)     .asInstanceOf[DatomicData]).as[Double]

      (DBigInt(BigInt(1))    .asInstanceOf[DatomicData]).as[BigInt]
      (DBigDec(BigDecimal(1)).asInstanceOf[DatomicData]).as[BigDecimal]

      (DInstant(new Date).asInstanceOf[DatomicData]).as[Date]

      (DUuid(UUID.randomUUID()).asInstanceOf[DatomicData]).as[UUID]

      (DUri(new URI("urn:isbn:096139210x")).asInstanceOf[DatomicData]).as[URI]

      (DBytes(Array(Byte.MinValue)).asInstanceOf[DatomicData]).as[Array[Byte]]

      success
    }

    "read subtypes of DatomicData as its underlying Scala type" in {

      DString("string").as[String]
      DBoolean(true)   .as[Boolean]
      DLong(1L)        .as[Long]
      DFloat(1.0f)     .as[Float]
      DDouble(1.0)     .as[Double]

      DBigInt(BigInt(1))    .as[BigInt]
      DBigDec(BigDecimal(1)).as[BigDecimal]

      DInstant(new Date).as[Date]

      DUuid(UUID.randomUUID()).as[UUID]

      DUri(new URI("urn:isbn:096139210x")).as[URI]

      DBytes(Array(Byte.MinValue)).as[Array[Byte]]

      success
    }

    "catch bad conversions" in {
      { DString("string").as[DLong] } must throwA[ClassCastException]
      success
    }

  }

  "ToDatomicCast" can {

    "write DatomicData as itself" in {
      toDatomic(DString("string")) must beAnInstanceOf[DString]
      toDatomic(DBoolean(true))    must beAnInstanceOf[DBoolean]
      toDatomic(DLong(1L))         must beAnInstanceOf[DLong]
      toDatomic(DFloat(1.0f))      must beAnInstanceOf[DFloat]
      toDatomic(DDouble(1.0))      must beAnInstanceOf[DDouble]

      toDatomic(DBigInt(BigInt(1)))     must beAnInstanceOf[DBigInt]
      toDatomic(DBigDec(BigDecimal(1))) must beAnInstanceOf[DBigDec]

      toDatomic(DInstant(new Date)) must beAnInstanceOf[DInstant]

      toDatomic(DUuid(UUID.randomUUID())) must beAnInstanceOf[DUuid]

      toDatomic(DUri(new URI("urn:isbn:096139210x"))) must beAnInstanceOf[DUri]

      toDatomic(DBytes(Array(Byte.MinValue))) must beAnInstanceOf[DBytes]

      success
    }

    "write Scala types as DatomicData" in {
      toDatomic("string") must beAnInstanceOf[DString]
      toDatomic(true)     must beAnInstanceOf[DBoolean]

      toDatomic(Long .MinValue) must beAnInstanceOf[DLong]
      toDatomic(Int  .MinValue) must beAnInstanceOf[DLong]
      toDatomic(Short.MinValue) must beAnInstanceOf[DLong]
      toDatomic(Char .MinValue) must beAnInstanceOf[DLong]
      toDatomic(Byte .MinValue) must beAnInstanceOf[DLong]

      toDatomic(1.0f) must beAnInstanceOf[DFloat]
      toDatomic(1.0)  must beAnInstanceOf[DDouble]

      toDatomic(BigInt(1))            must beAnInstanceOf[DBigInt]
      toDatomic(BigInt(1).bigInteger) must beAnInstanceOf[DBigInt]

      toDatomic(BigDecimal(1))            must beAnInstanceOf[DBigDec]
      toDatomic(BigDecimal(1).bigDecimal) must beAnInstanceOf[DBigDec]

      toDatomic(new Date) must beAnInstanceOf[DInstant]

      toDatomic(UUID.randomUUID()) must beAnInstanceOf[DUuid]

      toDatomic(new URI("urn:isbn:096139210x")) must beAnInstanceOf[DUri]

      toDatomic(Array(Byte.MinValue)) must beAnInstanceOf[DBytes]

      success
    }
  }

  import DatomicMapping._

  val ns = Namespace("test")
  val attrstring  = Attribute(ns / "string",  SchemaType.string,  Cardinality.one)
  val attrboolean = Attribute(ns / "boolean", SchemaType.boolean, Cardinality.one)
  val attrlong    = Attribute(ns / "long",    SchemaType.long,    Cardinality.one)
  val attrbigint  = Attribute(ns / "bigint",  SchemaType.bigint,  Cardinality.one)
  val attrfloat   = Attribute(ns / "float",   SchemaType.float,   Cardinality.one)
  val attrdouble  = Attribute(ns / "double",  SchemaType.double,  Cardinality.one)
  val attrbigdec  = Attribute(ns / "bigdec",  SchemaType.bigdec,  Cardinality.one)
  val attrinstant = Attribute(ns / "instant", SchemaType.instant, Cardinality.one)
  val attruuid    = Attribute(ns / "uuid",    SchemaType.uuid,    Cardinality.one)
  val attruri     = Attribute(ns / "uri",     SchemaType.uri,     Cardinality.one)
  val attrbytes   = Attribute(ns / "bytes",   SchemaType.bytes,   Cardinality.one)

  "FromDatomicInj" should {

    "uniquely determine the Scala type from the DatomicData type of an attribute" in {

      val entity = DEntity(null)

      /*
       * we simply need the following code to compile to test that
       * DDReader uniquely determines the output type
       * the collection of implicit DDReaders must combine to give
       * a function, not a relation from DatomicData to Scala types
       */
      {
        val string: String     = entity(attrstring)
        val boolean: Boolean   = entity(attrboolean)
        val long: Long         = entity(attrlong)
        val bigint: BigInt     = entity(attrbigint)
        val float: Float       = entity(attrfloat)
        val double: Double     = entity(attrdouble)
        val bigdec: BigDecimal = entity(attrbigdec)
        val instant: Date      = entity(attrinstant)
        val uuid: UUID         = entity(attruuid)
        val uri: URI           = entity(attruri)
        val bytes: Array[Byte] = entity(attrbytes)
      } must throwA[NullPointerException]

      success
    }
  }

  "FromDatomic" should {
    import DatomicMapping._

    "cast to a specific Scala type from the DatomicData type of an attribute" in {

      val entity = DEntity(null)

      {
        // core
        val string:  String      = entity.read[String]     (attrstring)
        val boolean: Boolean     = entity.read[Boolean]    (attrboolean)
        val long:    Long        = entity.read[Long]       (attrlong)
        val bigint:  BigInt      = entity.read[BigInt]     (attrbigint)
        val float:   Float       = entity.read[Float]      (attrfloat)
        val double:  Double      = entity.read[Double]     (attrdouble)
        val bigdec:  BigDecimal  = entity.read[BigDecimal] (attrbigdec)
        val instant: Date        = entity.read[Date]       (attrinstant)
        val uuid:    UUID        = entity.read[UUID]       (attruuid)
        val uri:     URI         = entity.read[URI]        (attruri)
        val bytes:   Array[Byte] = entity.read[Array[Byte]](attrbytes)

        // extensions
        val int:   Int   = entity.read[Int]  (attrlong)
        val short: Short = entity.read[Short](attrlong)
        val char:  Char  = entity.read[Char] (attrlong)
        val byte:  Byte  = entity.read[Byte] (attrlong)

        val jbigint: JBigInt     = entity.read[JBigInt]    (attrbigint)
        val jbigdec: JBigDecimal = entity.read[JBigDecimal](attrbigdec)
      } must throwA[NullPointerException]
    }

    "read the specific DatomicData type of an attribute" in {

      val entity = DEntity(null)

      {
        // core
        val string:  DString  = entity.read(attrstring)
        val boolean: DBoolean = entity.read(attrboolean)
        val long:    DLong    = entity.read(attrlong)
        val bigint:  DBigInt  = entity.read(attrbigint)
        val float:   DFloat   = entity.read(attrfloat)
        val double:  DDouble  = entity.read(attrdouble)
        val bigdec:  DBigDec  = entity.read(attrbigdec)
        val instant: DInstant = entity.read(attrinstant)
        val uuid:    DUuid    = entity.read(attruuid)
        val uri:     DUri     = entity.read(attruri)
        val bytes:   DBytes   = entity.read(attrbytes)
      } must throwA[NullPointerException]
    }
  }

  "ToDatomic" should {

    "support Scala types when asserting schema supported facts" in {
      val id = 1L

      /*
       * we simply need the following code to compile
       */
      // core
      SchemaFact.add(id)(attrstring  -> "str")
      SchemaFact.add(id)(attrboolean -> true)
      SchemaFact.add(id)(attrlong    -> 1L)
      SchemaFact.add(id)(attrbigint  -> BigInt(1))
      SchemaFact.add(id)(attrfloat   -> 1.0f)
      SchemaFact.add(id)(attrdouble  -> 1.0)
      SchemaFact.add(id)(attrbigdec  -> BigDecimal(1))
      SchemaFact.add(id)(attrinstant -> new Date)
      SchemaFact.add(id)(attruuid    -> UUID.randomUUID())
      SchemaFact.add(id)(attruri     -> new URI("urn:isbn:096139210x"))
      SchemaFact.add(id)(attrbytes   -> Array(Byte.MinValue))

      // extensions
      SchemaFact.add(id)(attrlong -> Int.MinValue)
      SchemaFact.add(id)(attrlong -> Short.MinValue)
      SchemaFact.add(id)(attrlong -> Char.MinValue)
      SchemaFact.add(id)(attrlong -> Byte.MinValue)

      SchemaFact.add(id)(attrbigint -> BigInt(1).bigInteger)
      SchemaFact.add(id)(attrbigdec -> BigDecimal(1).bigDecimal)

      success
    }
  }
}
