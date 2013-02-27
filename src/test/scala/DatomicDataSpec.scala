import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import Datomic._

@RunWith(classOf[JUnitRunner])
class DatomicDataSpec extends Specification {
  "DatomicData" should {
    "extract KW from DRef" in {
      val ns = Namespace("foo")
      DRef(ns / "bar") match {
        case DRef.IsKeyword(kw) if kw == KW(":foo/bar") => success
        case _ => failure
      }
    }
    "extract ID from DRef" in {
      DRef(DId(Partition.USER)) match {
        case DRef.IsId(id) => success
        case _ => failure
      }
    }

    "read/write BigDecimal" in {
      val bigdec = BigDecimal("123456789.12345789")
      val javabigdec = new java.math.BigDecimal("123456789.12345789")

      Datomic.fromDatomic[BigDecimal](Datomic.toDatomic(bigdec)) must beEqualTo(bigdec)
      Datomic.fromDatomic[java.math.BigDecimal](Datomic.toDatomic(javabigdec)) must beEqualTo(javabigdec)
    }

    "read/write BigInt" in {
      val bigint = BigInt("12345678912345789")
      val javabigdec = new java.math.BigInteger("12345678912345789")

      Datomic.fromDatomic[BigInt](Datomic.toDatomic(bigint)) must beEqualTo(bigint)
      Datomic.fromDatomic[java.math.BigInteger](Datomic.toDatomic(javabigdec)) must beEqualTo(javabigdec)
    }
  }
}