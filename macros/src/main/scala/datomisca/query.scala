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

import ast._
import macros.{QueryMacros}

import java.{util => ju}


/* DATOMIC QUERY */
trait Query {
  def find: Find
  def wizz: Option[With] = None
  def in: Option[In] = None
  def where: Where

  override def toString = s"""[ $find ${wizz.map( _.toString + " " ).getOrElse("")}${in.map( _.toString + " " ).getOrElse("")}$where ]"""
}

object Query extends QueryMacros {
  def apply(find: Find, where: Where): PureQuery = PureQuery(find, None, None, where)
  def apply(find: Find, in: In, where: Where): PureQuery = PureQuery(find, None, Some(in), where)
  def apply(find: Find, in: Option[In], where: Where): PureQuery = PureQuery(find, None, in, where)
  def apply(find: Find, wizz: With, in: In, where: Where): PureQuery = PureQuery(find, Some(wizz), Some(in), where)
  def apply(find: Find, wizz: Option[With], in: Option[In], where: Where): PureQuery = PureQuery(find, wizz, in, where)
}

final case class PureQuery(override val find: Find, override val wizz: Option[With] = None, override val in: Option[In] = None, override val where: Where) extends Query

abstract class TypedQueryAuto(query: PureQuery) extends Query {
  override def find = query.find
  override def wizz = query.wizz
  override def in = query.in
  override def where = query.where
}
