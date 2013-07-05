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

case object SchemaTypeString extends SchemaType[DString] {
  def keyword = Keyword(Namespace.DB.TYPE, "string")
}

case object SchemaTypeBoolean extends SchemaType[DBoolean] {
  def keyword = Keyword(Namespace.DB.TYPE, "boolean")
}

case object SchemaTypeLong extends SchemaType[DLong] {
  def keyword = Keyword(Namespace.DB.TYPE, "long")
}

case object SchemaTypeBigInt extends SchemaType[DBigInt] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigint")
}

case object SchemaTypeFloat extends SchemaType[DFloat] {
  def keyword = Keyword(Namespace.DB.TYPE, "float")
}

case object SchemaTypeDouble extends SchemaType[DDouble] {
  def keyword = Keyword(Namespace.DB.TYPE, "double")
}

case object SchemaTypeBigDec extends SchemaType[DBigDec] {
  def keyword = Keyword(Namespace.DB.TYPE, "bigdec")
}

case object SchemaTypeRef extends SchemaType[DRef] {
  def keyword = Keyword(Namespace.DB.TYPE, "ref")
}

case object SchemaTypeInstant extends SchemaType[DInstant] {
  def keyword = Keyword(Namespace.DB.TYPE, "instant")
}

case object SchemaTypeUuid extends SchemaType[DUuid] {
  def keyword = Keyword(Namespace.DB.TYPE, "uuid")
}

case object SchemaTypeUri extends SchemaType[DUri] {
  def keyword = Keyword(Namespace.DB.TYPE, "uri")
}

case object SchemaTypeBytes extends SchemaType[DBytes] {
  def keyword = Keyword(Namespace.DB.TYPE, "bytes")
}

case object SchemaTypeKeyword extends SchemaType[DKeyword] {
  def keyword = Keyword(Namespace.DB.TYPE, "keyword")
}

//case class SchemaType(keyword: Keyword)

object SchemaType {
  val string = SchemaTypeString //SchemaType(Keyword(Namespace.DB.TYPE, "string"))
  val boolean = SchemaTypeBoolean //SchemaType(Keyword(Namespace.DB.TYPE, "boolean"))
  val long = SchemaTypeLong //SchemaType(Keyword(Namespace.DB.TYPE, "long"))
  val bigint = SchemaTypeBigInt //SchemaType(Keyword(Namespace.DB.TYPE, "bigint"))
  val float = SchemaTypeFloat //SchemaType(Keyword(Namespace.DB.TYPE, "float"))
  val double = SchemaTypeDouble //SchemaType(Keyword(Namespace.DB.TYPE, "double"))
  val bigdec = SchemaTypeBigDec //SchemaType(Keyword(Namespace.DB.TYPE, "bigdec"))
  val ref = SchemaTypeRef //SchemaType(Keyword(Namespace.DB.TYPE, "ref"))
  val instant = SchemaTypeInstant //SchemaType(Keyword(Namespace.DB.TYPE, "instant"))
  val uuid = SchemaTypeUuid //SchemaType(Keyword(Namespace.DB.TYPE, "uuid"))
  val uri = SchemaTypeUri //SchemaType(Keyword(Namespace.DB.TYPE, "uri"))
  val bytes = SchemaTypeBytes //SchemaType(Keyword(Namespace.DB.TYPE, "bytes"))
  val keyword = SchemaTypeKeyword
}

trait Cardinality {
  def keyword: Keyword
}

case object CardinalityOne extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "one")
}

case object CardinalityMany extends Cardinality {
  def keyword = Keyword(Namespace.DB.CARDINALITY, "many")
}

//case class Cardinality(keyword: Keyword)

object Cardinality {
  val one = CardinalityOne //Cardinality(Keyword(Namespace.DB.CARDINALITY, "one"))
  val many = CardinalityMany //Cardinality(Keyword(Namespace.DB.CARDINALITY, "many"))
}

case class Unique(keyword: Keyword)

object Unique {
  val value = Unique(Keyword(Namespace.DB.UNIQUE, "value"))
  val identity = Unique(Keyword(Namespace.DB.UNIQUE, "identity"))
}
