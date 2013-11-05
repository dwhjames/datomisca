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

import scala.util.parsing.input.Positional


/* DATOMIC RULES */
trait Rule extends Positional

trait Term

final case class Var(name: String) extends Term {
  override def toString = "?" + name
}

final case class Keyword(override val name: String, override val ns: Option[Namespace] = None) extends Term with Namespaceable with Positional

object Keyword {
  def apply(name: String, ns: Namespace) = new Keyword(name, Some(ns))
  def apply(ns: Namespace, name: String) = new Keyword(name, Some(ns))

  def apply(kw: clojure.lang.Keyword) = new Keyword(kw.getName, Some(Namespace(kw.getNamespace)))
}

final case class Const(underlying: DatomicData) extends Term {
  override def toString = underlying.toString
}

case object Empty extends Term {
  override def toString = "_"
}

sealed trait DataSource extends Term {
  def name: String

  override def toString = "$" + name
}

final case class ExternalDS(override val name: String) extends DataSource

case object ImplicitDS extends DataSource {
  def name = ""
}
