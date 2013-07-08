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


trait SchemaType[DD <: DatomicData] {
  def keyword: Keyword
}

object SchemaType {
  object string extends SchemaType[DString] {
    def keyword = Keyword(Namespace.DB.TYPE, "string")
  }

  object boolean extends SchemaType[DBoolean] {
    def keyword = Keyword(Namespace.DB.TYPE, "boolean")
  }

  object long extends SchemaType[DLong] {
    def keyword = Keyword(Namespace.DB.TYPE, "long")
  }

  object bigint extends SchemaType[DBigInt] {
    def keyword = Keyword(Namespace.DB.TYPE, "bigint")
  }

  object float extends SchemaType[DFloat] {
    def keyword = Keyword(Namespace.DB.TYPE, "float")
  }

  object double extends SchemaType[DDouble] {
    def keyword = Keyword(Namespace.DB.TYPE, "double")
  }

  object bigdec extends SchemaType[DBigDec] {
    def keyword = Keyword(Namespace.DB.TYPE, "bigdec")
  }

  object ref extends SchemaType[DRef] {
    def keyword = Keyword(Namespace.DB.TYPE, "ref")
  }

  object instant extends SchemaType[DInstant] {
    def keyword = Keyword(Namespace.DB.TYPE, "instant")
  }

  object uuid extends SchemaType[DUuid] {
    def keyword = Keyword(Namespace.DB.TYPE, "uuid")
  }

  object uri extends SchemaType[DUri] {
    def keyword = Keyword(Namespace.DB.TYPE, "uri")
  }

  object bytes extends SchemaType[DBytes] {
    def keyword = Keyword(Namespace.DB.TYPE, "bytes")
  }

  object keyword extends SchemaType[DKeyword] {
    def keyword = Keyword(Namespace.DB.TYPE, "keyword")
  }
}

trait Cardinality {
  def keyword: Keyword
}

object Cardinality {
  case object one extends Cardinality {
    def keyword = Keyword(Namespace.DB.CARDINALITY, "one")
  }

  case object many extends Cardinality {
    def keyword = Keyword(Namespace.DB.CARDINALITY, "many")
  }
}

case class Unique(keyword: Keyword)

object Unique {
  val value = Unique(Keyword(Namespace.DB.UNIQUE, "value"))
  val identity = Unique(Keyword(Namespace.DB.UNIQUE, "identity"))
}
