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

import java.{lang => jl}
import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.util.{Date, UUID}
import java.net.URI


object DatomicRef

sealed trait SchemaType[DD] {
  val keyword: Keyword
}

object SchemaType {
  object string extends SchemaType[String] {
    val keyword = Namespace.DB.TYPE / "string"
  }

  object boolean extends SchemaType[jl.Boolean] {
    val keyword = Namespace.DB.TYPE / "boolean"
  }

  object long extends SchemaType[jl.Long] {
    val keyword = Namespace.DB.TYPE / "long"
  }

  object bigint extends SchemaType[JBigInt] {
    val keyword = Namespace.DB.TYPE / "bigint"
  }

  object float extends SchemaType[jl.Float] {
    val keyword = Namespace.DB.TYPE / "float"
  }

  object double extends SchemaType[jl.Double] {
    val keyword = Namespace.DB.TYPE / "double"
  }

  object bigdec extends SchemaType[JBigDecimal] {
    val keyword = Namespace.DB.TYPE / "bigdec"
  }

  object ref extends SchemaType[DatomicRef.type] {
    val keyword = Namespace.DB.TYPE / "ref"
  }

  object instant extends SchemaType[Date] {
    val keyword = Namespace.DB.TYPE / "instant"
  }

  object uuid extends SchemaType[UUID] {
    val keyword = Namespace.DB.TYPE / "uuid"
  }

  object uri extends SchemaType[URI] {
    val keyword = Namespace.DB.TYPE / "uri"
  }

  object bytes extends SchemaType[Array[Byte]] {
    val keyword = Namespace.DB.TYPE / "bytes"
  }

  object keyword extends SchemaType[Keyword] {
    val keyword = Namespace.DB.TYPE / "keyword"
  }
}

sealed trait Cardinality {
  val keyword: Keyword
}

object Cardinality {
  case object one extends Cardinality {
    val keyword = Namespace.DB.CARDINALITY / "one"
  }

  case object many extends Cardinality {
    val keyword = Namespace.DB.CARDINALITY / "many"
  }
}

sealed trait Unique {
  val keyword: Keyword
}

object Unique {
  case object value extends Unique {
    val keyword = Namespace.DB.UNIQUE / "value"
  }
  case object identity extends Unique {
    val keyword = Namespace.DB.UNIQUE / "identity"
  }
}
