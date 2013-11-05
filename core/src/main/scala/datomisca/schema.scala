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


sealed trait SchemaType[DD <: DatomicData] {
  val keyword: Keyword
}

object SchemaType {
  object string extends SchemaType[DString] {
    val keyword = Keyword(Namespace.DB.TYPE, "string")
  }

  object boolean extends SchemaType[DBoolean] {
    val keyword = Keyword(Namespace.DB.TYPE, "boolean")
  }

  object long extends SchemaType[DLong] {
    val keyword = Keyword(Namespace.DB.TYPE, "long")
  }

  object bigint extends SchemaType[DBigInt] {
    val keyword = Keyword(Namespace.DB.TYPE, "bigint")
  }

  object float extends SchemaType[DFloat] {
    val keyword = Keyword(Namespace.DB.TYPE, "float")
  }

  object double extends SchemaType[DDouble] {
    val keyword = Keyword(Namespace.DB.TYPE, "double")
  }

  object bigdec extends SchemaType[DBigDec] {
    val keyword = Keyword(Namespace.DB.TYPE, "bigdec")
  }

  object ref extends SchemaType[DRef] {
    val keyword = Keyword(Namespace.DB.TYPE, "ref")
  }

  object instant extends SchemaType[DInstant] {
    val keyword = Keyword(Namespace.DB.TYPE, "instant")
  }

  object uuid extends SchemaType[DUuid] {
    val keyword = Keyword(Namespace.DB.TYPE, "uuid")
  }

  object uri extends SchemaType[DUri] {
    val keyword = Keyword(Namespace.DB.TYPE, "uri")
  }

  object bytes extends SchemaType[DBytes] {
    val keyword = Keyword(Namespace.DB.TYPE, "bytes")
  }

  object keyword extends SchemaType[DKeyword] {
    val keyword = Keyword(Namespace.DB.TYPE, "keyword")
  }
}

sealed trait Cardinality {
  val keyword: Keyword
}

object Cardinality {
  case object one extends Cardinality {
    val keyword = Keyword(Namespace.DB.CARDINALITY, "one")
  }

  case object many extends Cardinality {
    val keyword = Keyword(Namespace.DB.CARDINALITY, "many")
  }
}

sealed trait Unique {
  val keyword: Keyword
}

object Unique {
  case object value extends Unique {
    val keyword = Keyword(Namespace.DB.UNIQUE, "value")
  }
  case object identity extends Unique {
    val keyword = Keyword(Namespace.DB.UNIQUE, "identity")
  }
}
