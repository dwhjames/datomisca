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
package macros

import scala.language.experimental.macros


private[datomisca] trait QueryMacros {

  /** Parse a string as a Datalog query.
    *
    * {{{
    * val query = Query("""
    *   [:find ?e
    *    :where [?e :db/doc]]
    * """)
    * }}}
    *
    * Implemented as a macro. The string is parsed at compile time,
    * so any parsing errors will result in compliation failure. The
    * parsing checks that the string is valid EDN, and a further round
    * of minimal validate is performed to ensure that the EDN has the
    * expected structure to be a Datalog query.
    *
    * The macro also determines the input and output arity of the query.
    * The declared return type of the macro is [[AbstractQuery]], but the
    * concrete return type is of the form
    *   {{{ TypedQueryN[AnyRef, ..., TupleN[Any, ...]] }}}
    *
    *
    * When the resulting query is executed, it returns a `Iterable[TupleN[Any, ...]]`
    *
    * @param edn a Datalog query as a string
    * @return an arity-typed query as a data structure
    */
  def apply(edn: String): AbstractQuery = macro MacroImpl.cljQueryImpl


  /** Parse a string a collection of Datalog rules.
    *
    * Macro-based helper to create Rule alias to be used in Queries.
    * {{{
    * val rules = Datomic.Query.rules("""
    *   [[[doc ?e ?d]
    *     [?e :db/doc ?d]]]
    * """)
    * }}}
    *
    * Implemented as a macro. The string is parse at compile time,
    * so any parsing errors will result in complation failure.
    *
    * @param edn Datalog rules as a string
    * @return Datalog rules as a data structure
    */
  def rules(edn: String): QueryRules = macro MacroImpl.cljRulesImpl
}
