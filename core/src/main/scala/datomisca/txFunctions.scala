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

import clojure.lang.Keyword


/*
 * Construct a database function intended to be used as a transaction function.
 * The key point being that the first argument is assumed to the the database value.
 * The `typed` builder functions assume this and construct a TypedAddDbFunction for
 * the remaining arguments.
 */
object AddTxFunction extends AddTxFunctionGen {
  def apply(kw: Keyword)
           (params: String*)
           (lang: String, partition: Partition = Partition.USER, imports: String = "", requires: String = "")
           (code: String) =
    new AddDbFunction(kw, lang, params, code, imports, requires, partition)


  def typed(kw: Keyword)
           (param: String)
           (lang: String, partition: Partition = Partition.USER, imports: String = "", requires: String = "")
           (code: String) =
    new gen.TypedAddDbFunction0(
      new AddDbFunction(kw, lang, Seq(param), code))

  // plus generated code
}

class InvokeTxFunction(
    fn:   Keyword,
    args: Seq[AnyRef]
) extends TxData {
  def toTxData: AnyRef = {
    datomic.Util.list(
      (fn +: args): _*
    )
  }
}

object InvokeTxFunction extends InvokeTxFunctionGen {
  def apply(fn: Keyword)(args: AnyRef*) = new InvokeTxFunction(fn, args)

  def apply(fn: gen.TypedAddDbFunction0)() =
    new InvokeTxFunction(fn.ident, Seq.empty)

}

