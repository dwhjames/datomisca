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


class AddDbFunction(
    val ident: Keyword,
    lang:      String,
    params:    Seq[String],
    code:      String,
    imports:   String    = "",
    requires:  String    = "",
    partition: Partition = Partition.USER
) extends Operation with KeywordIdentified {
  val kw = Keyword("add", Some(Namespace.DB))

  def toNative: AnyRef =
    datomic.Util.map(
      Keyword("id", Some(Namespace.DB)).toNative,    DId(partition).toNative,
      Keyword("ident", Some(Namespace.DB)).toNative, ident.toNative,
      Keyword("fn", Some(Namespace.DB)).toNative,    datomic.Peer.function(
        datomic.Util.map(
          Keyword("lang").toNative,     lang,
          Keyword("imports").toNative,  datomic.Util.read(imports),
          Keyword("requires").toNative, datomic.Util.read(requires),
          Keyword("params").toNative,   datomic.Util.list(params: _*),
          Keyword("code").toNative,     code
        )
      )
    )

  override def toString = toNative.toString
}

abstract class TypedAddDbFunction(fn: AddDbFunction) extends Operation with KeywordIdentified {
  val ident = fn.ident
  // lazy val ref = fn.ref
  val kw = fn.kw

  def toNative: AnyRef = fn.toNative
}
// TypedAddDbFunction 0-22 in managed source

/*
 * Construct a vanila database function.
 */
object AddDbFunction {
  def apply(kw: Keyword)
           (params: String*)
           (lang: String, partition: Partition = Partition.USER, imports: String = "", requires: String = "")
           (code: String) =
    new AddDbFunction(kw, lang, params, code, imports, requires, partition)
}
