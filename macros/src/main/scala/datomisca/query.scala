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

abstract class AbstractQuery(val query: clojure.lang.IPersistentMap)

final class QueryRules(val edn: clojure.lang.PersistentVector) extends AnyVal {
  override def toString = edn.toString

  def ++(that: QueryRules): QueryRules = {
    var t: clojure.lang.ITransientCollection = this.edn.asTransient()
    val i = that.edn.iterator()
    while (i.hasNext) {
      t = t.conj(i.next())
    }
    new QueryRules(t.persistent().asInstanceOf[clojure.lang.PersistentVector])
  }
}

object Query extends macros.QueryMacros
