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

import scala.util.parsing.input.Positional


case class Namespace(name: String) {
  override def toString = name

  def /(name: String) = Keyword(name, Some(this))
}

object Namespace {
  val DB = new Namespace("db") {
    val PART = Namespace("db.part")
    val TYPE = Namespace("db.type")
    val CARDINALITY = Namespace("db.cardinality")
    val INSTALL = Namespace("db.install")
    val UNIQUE = Namespace("db.unique")
    val FN = Namespace("db.fn")
    val EXCISE = Namespace("db.excise")
  } 
}

trait Nativeable {
  def toNative: AnyRef
}

trait Namespaceable extends Nativeable {
  def name: String
  def ns: Option[Namespace] = None

  override def toString = ":" + ( if(ns.isDefined) {ns.get + "/"} else "" ) + name

  def toNative: AnyRef = clojure.lang.Keyword.intern(( if(ns.isDefined) {ns.get + "/"} else "" ) + name )
}

trait Term

case class Var(name: String) extends Term {
  override def toString = "?" + name
}

case class Keyword(override val name: String, override val ns: Option[Namespace] = None) extends Term with Namespaceable with Positional

object Keyword {
  def apply(name: String, ns: Namespace) = new Keyword(name, Some(ns))
  def apply(ns: Namespace, name: String) = new Keyword(name, Some(ns))

  def apply(kw: clojure.lang.Keyword) = new Keyword(kw.getName, Some(Namespace(kw.getNamespace)))
}

case class Const(underlying: DatomicData) extends Term {
  override def toString = underlying.toString
}

case object Empty extends Term {
  override def toString = "_"
}

trait DataSource extends Term {
  def name: String

  override def toString = "$" + name
}

case class ExternalDS(override val name: String) extends DataSource

case object ImplicitDS extends DataSource {
  def name = ""
}

trait TempIdentified {
  def id: DId
}

trait FinalIdentified {
  def id: Long
}

trait KeywordIdentified {
  def ident: Keyword
}

case class Partition(keyword: Keyword) {
  override def toString = keyword.toString
}

object Partition {
  val DB = Partition(Keyword("db", Some(Namespace.DB.PART)))
  val TX = Partition(Keyword("tx", Some(Namespace.DB.PART)))
  val USER = Partition(Keyword("user", Some(Namespace.DB.PART)))
}
