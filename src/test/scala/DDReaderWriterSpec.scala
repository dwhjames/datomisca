
import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import Datomic.toDatomic

import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI

@RunWith(classOf[JUnitRunner])
class DDReaderWriterSpec extends Specification {

  "DDReader" can {

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
      (DBigInt(BigInt(1))    .asInstanceOf[DatomicData]).as[JBigInt]

      (DBigDec(BigDecimal(1)).asInstanceOf[DatomicData]).as[BigDecimal]
      (DBigDec(BigDecimal(1)).asInstanceOf[DatomicData]).as[JBigDecimal]

      (DInstant(new Date).asInstanceOf[DatomicData]).as[Date]

      (DUuid(UUID.randomUUID()).asInstanceOf[DatomicData]).as[UUID]

      (DUri(new URI("urn:isbn:096139210x")).asInstanceOf[DatomicData]).as[URI]

      (DBytes(Array(Byte.MinValue)).asInstanceOf[DatomicData]).as[Array[Byte]]

      success
    }

    "catch bad conversions" in {
      { DString("string").as[DLong] } must throwA[ClassCastException]
      success
    }

  }

  "DDWriter" can {

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
      toDatomic(1L)       must beAnInstanceOf[DLong]
      toDatomic(1.0f)     must beAnInstanceOf[DFloat]
      toDatomic(1.0)      must beAnInstanceOf[DDouble]

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
}
