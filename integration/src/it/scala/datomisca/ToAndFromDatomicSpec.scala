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

import scala.language.reflectiveCalls

import org.scalatest.{FlatSpec, Matchers}

import Datomic.{toDatomic, fromDatomic}

import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI


class ToFromDatomicSpec extends FlatSpec with Matchers {

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

  val attrref     = Attribute(ns / "ref",     SchemaType.ref,     Cardinality.one)
  val attrrefMany = Attribute(ns / "refMany", SchemaType.ref,     Cardinality.many)


  "ToDatomicCast" can "write DatomicData as itself" in {
    toDatomic(DString("string")) shouldBe a [DString]
    toDatomic(DBoolean(true))    shouldBe a [DBoolean]
    toDatomic(DLong(1L))         shouldBe a [DLong]
    toDatomic(DFloat(1.0f))      shouldBe a [DFloat]
    toDatomic(DDouble(1.0))      shouldBe a [DDouble]

    toDatomic(DBigInt(BigInt(1)))     shouldBe a [DBigInt]
    toDatomic(DBigDec(BigDecimal(1))) shouldBe a [DBigDec]

    toDatomic(DInstant(new Date)) shouldBe a [DInstant]

    toDatomic(DUuid(UUID.randomUUID())) shouldBe a [DUuid]

    toDatomic(DUri(new URI("urn:isbn:096139210x"))) shouldBe a [DUri]

    toDatomic(DBytes(Array(Byte.MinValue))) shouldBe a [DBytes]

    toDatomic(DKeyword(Datomic.KW(":my-kw"))) shouldBe a [DKeyword]

  }


  it can "write Scala types as DatomicData" in {
    toDatomic("string") shouldBe a [DString]
    toDatomic(true)     shouldBe a [DBoolean]

    toDatomic(Long .MinValue) shouldBe a [DLong]
    toDatomic(Int  .MinValue) shouldBe a [DLong]
    toDatomic(Short.MinValue) shouldBe a [DLong]
    toDatomic(Char .MinValue) shouldBe a [DLong]
    toDatomic(Byte .MinValue) shouldBe a [DLong]

    toDatomic(1.0f) shouldBe a [DFloat]
    toDatomic(1.0)  shouldBe a [DDouble]

    toDatomic(BigInt(1))            shouldBe a [DBigInt]
    toDatomic(BigInt(1).bigInteger) shouldBe a [DBigInt]

    toDatomic(BigDecimal(1))            shouldBe a [DBigDec]
    toDatomic(BigDecimal(1).bigDecimal) shouldBe a [DBigDec]

    toDatomic(new Date) shouldBe a [DInstant]

    toDatomic(UUID.randomUUID()) shouldBe a [DUuid]

    toDatomic(new URI("urn:isbn:096139210x")) shouldBe a [DUri]

    toDatomic(Array(Byte.MinValue)) shouldBe a [DBytes]

    toDatomic(Datomic.KW(":my-kw")) shouldBe a [DKeyword]

  }


  "ToDatomic" should "support Scala types when asserting schema supported facts" in {
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
    SchemaFact.add(id)(attrkeyword -> Datomic.KW(":my-kw"))

    // extensions
    SchemaFact.add(id)(attrlong -> Int.MinValue)
    SchemaFact.add(id)(attrlong -> Short.MinValue)
    SchemaFact.add(id)(attrlong -> Char.MinValue)
    SchemaFact.add(id)(attrlong -> Byte.MinValue)

    SchemaFact.add(id)(attrbigint -> BigInt(1).bigInteger)
    SchemaFact.add(id)(attrbigdec -> BigDecimal(1).bigDecimal)

  }

  it should "support various ways to write to reference attributes" in {
    val id = 1L

    /*
     * we simply need the following code to compile
     */
    SchemaFact.add(id)(attrref -> DRef(Datomic.KW(":my-kw")))
    SchemaFact.add(id)(attrref -> DRef(1L))
    SchemaFact.add(id)(attrref -> DRef(DId(1L)))
    SchemaFact.add(id)(attrref -> DRef(DId(Partition.USER)))

    SchemaFact.add(id)(attrref -> DId(1L))
    SchemaFact.add(id)(attrref -> DId(Partition.USER))
    SchemaFact.add(id)(attrref -> 1L)
    SchemaFact.add(id)(attrref -> Datomic.KW(":my-kw"))

    SchemaFact.add(id)(attrrefMany -> Set(DId(1L)))
    SchemaFact.add(id)(attrrefMany -> Seq(DId(Partition.USER)))
    SchemaFact.add(id)(attrrefMany -> List(1L))
    SchemaFact.add(id)(attrrefMany -> Iterable(Datomic.KW(":my-kw")))

    SchemaFact.add(id)(attrrefMany -> DId(1L))
    SchemaFact.add(id)(attrrefMany -> DId(Partition.USER))
    SchemaFact.add(id)(attrrefMany -> 1L)
    SchemaFact.add(id)(attrrefMany -> Datomic.KW(":my-kw"))

    val addfact = Fact.add(id)(attrstring.ident -> "str")
    SchemaFact.add(id)(attrref -> addfact)
    SchemaFact.add(id)(attrrefMany -> Set(addfact))

    val retractfact = Fact.retract(id)(attrstring.ident -> "str")
    SchemaFact.add(id)(attrref -> retractfact)
    SchemaFact.add(id)(attrrefMany -> Set(retractfact))

    val identEntity = AddIdent(Datomic.KW(":my-kw"))
    SchemaFact.add(id)(attrref -> identEntity)
    SchemaFact.add(id)(attrrefMany -> Set(identEntity))

  }


  "FromDatomicCast" can "read DatomicData as itself" in {

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

    DKeyword(Datomic.KW(":my-kw")).as[DKeyword]

  }

  it can "read DatomicData as its underlying Scala type" in {

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

    DKeyword(Datomic.KW(":my-kw")).as[Keyword]

  }


  it should "throw ClassCastException for bad conversions" in {
    a [ClassCastException] should be thrownBy DString("string").as[DLong]
  }


  "FromDatomic" can "cast to a specific Scala type from the DatomicData type of an attribute" in {

    val entity = DEntity(null)

    a [NullPointerException] should be thrownBy {
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
    }
  }

  it can "read the specific DatomicData type of an attribute" in {

    val entity = DEntity(null)

    a [NullPointerException] should be thrownBy {
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
    }

  }


  "FromDatomicInj" can "convert DatomicData into its underlying Scala type" in {

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

    val keyword: Keyword = fromDatomic(DKeyword(Datomic.KW(":my-kw")))

  }


  "FromDatomicInj" should "uniquely determine the Scala type from the DatomicData type of an attribute" in {

    val entity = DEntity(null)

    /*
     * we simply need the following code to compile to test that
     * DDReader uniquely determines the output type
     * the collection of implicit DDReaders must combine to give
     * a function, not a relation from DatomicData to Scala types
     */
    a [NullPointerException] should be thrownBy {
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
    }
  }
}
