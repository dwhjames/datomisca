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

trait Namespaceable extends Nativeable {
  def name: String
  def ns: Option[Namespace] = None

  override def toString = ":" + ( if(ns.isDefined) {ns.get + "/"} else "" ) + name

  def toNative: AnyRef = clojure.lang.Keyword.intern(( if(ns.isDefined) {ns.get + "/"} else "" ) + name )
}
