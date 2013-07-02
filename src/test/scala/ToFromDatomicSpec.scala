
import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import Datomic.{toDatomic, fromDatomic, KW}
import DatomicMapping._

import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI

@RunWith(classOf[JUnitRunner])
class ToFromDatomicSpec extends Specification {

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
  val attrkeyword = Attribute(ns / "keyword", SchemaType.keyword, Cardinality.one)

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

      toDatomic(DKeyword(KW(":my-kw"))) must beAnInstanceOf[DKeyword]

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

      toDatomic(KW(":my-kw")) must beAnInstanceOf[DKeyword]

      success
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
      SchemaFact.add(id)(attrkeyword -> KW(":my-kw"))

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

  "FromDatomicCast" can {

    "read DatomicData as itself" in {

      DString("string").as[DString]
      DBoolean(true)   .as[DBoolean]
      DLong(1L)        .as[DLong]
      DFloat(1.0f)     .as[DFloat]
      DDouble(1.0)     .as[DDouble]

      DBigInt(BigInt(1))    .as[DBigInt]
      DBigDec(BigDecimal(1)).as[DBigDec]

      DInstant(new Date).as[DInstant]

      DUuid(UUID.randomUUID()).as[DUuid]

      DUri(new URI("urn:isbn:096139210x")).as[DUri]

      DBytes(Array(Byte.MinValue)).as[DBytes]

      DKeyword(KW(":my-kw")).as[DKeyword]

      success
    }

    "read DatomicData as its underlying Scala type" in {

      DString("string").as[String]
      DBoolean(true)   .as[Boolean]
      DLong(1L)        .as[Long]
      DLong(1L)        .as[Int]
      DLong(1L)        .as[Short]
      DLong(1L)        .as[Char]
      DLong(1L)        .as[Byte]
      DFloat(1.0f)     .as[Float]
      DDouble(1.0)     .as[Double]

      DBigInt(BigInt(1))    .as[BigInt]
      DBigDec(BigDecimal(1)).as[BigDecimal]

      DBigInt(BigInt(1))    .as[JBigInt]
      DBigDec(BigDecimal(1)).as[JBigDecimal]

      DInstant(new Date).as[Date]

      DUuid(UUID.randomUUID()).as[UUID]

      DUri(new URI("urn:isbn:096139210x")).as[URI]

      DBytes(Array(Byte.MinValue)).as[Array[Byte]]

      DKeyword(KW(":my-kw")).as[Keyword]

      success
    }

    "bad conversions throw ClassCastException" in {
      { DString("string").as[DLong] } must throwA[ClassCastException]
      success
    }

  }

  "FromDatomic" can {

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
        val keyword: Keyword     = entity.read[Keyword]    (attrkeyword)

        // extensions
        val int:   Int   = entity.read[Int]  (attrlong)
        val short: Short = entity.read[Short](attrlong)
        val char:  Char  = entity.read[Char] (attrlong)
        val byte:  Byte  = entity.read[Byte] (attrlong)

        val jbigint: JBigInt     = entity.read[JBigInt]    (attrbigint)
        val jbigdec: JBigDecimal = entity.read[JBigDecimal](attrbigdec)
      } must throwA[NullPointerException]

      success
    }

    "read the specific DatomicData type of an attribute" in {

      val entity = DEntity(null)

      {
        // core
        val string  = entity.read[DString](attrstring)
        val boolean = entity.read[DBoolean](attrboolean)
        val long    = entity.read[DLong](attrlong)
        val bigint  = entity.read[DBigInt](attrbigint)
        val float   = entity.read[DFloat](attrfloat)
        val double  = entity.read[DDouble](attrdouble)
        val bigdec  = entity.read[DBigDec](attrbigdec)
        val instant = entity.read[DInstant](attrinstant)
        val uuid    = entity.read[DUuid](attruuid)
        val uri     = entity.read[DUri](attruri)
        val bytes   = entity.read[DBytes](attrbytes)
        val keyword = entity.read[DKeyword](attrkeyword)
      } must throwA[NullPointerException]

      success
    }
  }

  "FromDatomicInj" can {

    "convert DatomicData into its underlying Scala type" in {

      val string:  String  = fromDatomic(DString("string"))
      val boolean: Boolean = fromDatomic(DBoolean(true))
      val long:    Long    = fromDatomic(DLong(1L))
      val float:   Float   = fromDatomic(DFloat(1.0f))
      val double:  Double  = fromDatomic(DDouble(1.0))

      val bigint: BigInt     = fromDatomic(DBigInt(BigInt(1)))
      val bigdec: BigDecimal = fromDatomic(DBigDec(BigDecimal(1)))

      val date: Date = fromDatomic(DInstant(new Date))

      val uuid: UUID = fromDatomic(DUuid(UUID.randomUUID()))

      val uri: URI = fromDatomic(DUri(new URI("urn:isbn:096139210x")))

      val bytes: Array[Byte] = fromDatomic(DBytes(Array(Byte.MinValue)))

      val keyword: Keyword = fromDatomic(DKeyword(KW(":my-kw")))

      success
    }
  }

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
        val keyword: Keyword   = entity(attrkeyword)
      } must throwA[NullPointerException]

      success
    }
  }
}
