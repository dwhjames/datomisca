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

import java.{lang => jl}
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


  "ToDatomicCast" can "write Scala types as DatomicData" in {
    toDatomic("string") shouldBe a [String]
    toDatomic(true)     shouldBe a [jl.Boolean]

    toDatomic(Long .MinValue) shouldBe a [jl.Long]
    toDatomic(Int  .MinValue) shouldBe a [jl.Long]
    toDatomic(Short.MinValue) shouldBe a [jl.Long]
    toDatomic(Char .MinValue) shouldBe a [jl.Long]
    toDatomic(Byte .MinValue) shouldBe a [jl.Long]

    toDatomic(1.0f) shouldBe a [jl.Float]
    toDatomic(1.0)  shouldBe a [jl.Double]

    toDatomic(BigInt(1))            shouldBe a [JBigInt]
    toDatomic(BigInt(1).bigInteger) shouldBe a [JBigInt]

    toDatomic(BigDecimal(1))            shouldBe a [JBigDecimal]
    toDatomic(BigDecimal(1).bigDecimal) shouldBe a [JBigDecimal]

    toDatomic(new Date) shouldBe a [Date]

    toDatomic(UUID.randomUUID()) shouldBe a [UUID]

    toDatomic(new URI("urn:isbn:096139210x")) shouldBe a [URI]

    toDatomic(Array(Byte.MinValue)) shouldBe a [Array[Byte]]

    toDatomic(Datomic.KW(":my-kw")) shouldBe a [Keyword]

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


  "FromDatomic" can "cast to a specific Scala type from the DatomicData type of an attribute" in {

    val entity = new Entity(null)

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


  "FromDatomicInj" can "convert DatomicData into its underlying Scala type" in {

    val string:  String  = fromDatomic("string")
    val boolean: Boolean = fromDatomic(true: jl.Boolean)
    val long:    Long    = fromDatomic(1L: jl.Long)
    val float:   Float   = fromDatomic(1.0f: jl.Float)
    val double:  Double  = fromDatomic(1.0: jl.Double)

    val bigint: BigInt     = fromDatomic(BigInt(1).bigInteger)
    val bigdec: BigDecimal = fromDatomic(BigDecimal(1).bigDecimal)

    val date: Date = fromDatomic(new Date)

    val uuid: UUID = fromDatomic(UUID.randomUUID())

    val uri: URI = fromDatomic(new URI("urn:isbn:096139210x"))

    val bytes: Array[Byte] = fromDatomic(Array(Byte.MinValue))

    val keyword: Keyword = fromDatomic(Datomic.KW(":my-kw"))

  }


  "FromDatomicInj" should "uniquely determine the Scala type from the DatomicData type of an attribute" in {

    val entity = new Entity(null)

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
