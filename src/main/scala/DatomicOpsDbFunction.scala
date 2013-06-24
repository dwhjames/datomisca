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

import scala.collection.JavaConverters._

case class AddDbFunction(
    val ident: Keyword,
    lang:      String,
    params:    Seq[String],
    code:      String,
    imports:   String    = "",
    requires:  String    = "",
    partition: Partition = Partition.USER
) extends Operation with KeywordIdentified {
  lazy val ref = DRef(ident)
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
  lazy val ref = fn.ref
  val kw = fn.kw

  def toNative: AnyRef = fn.toNative
}

case class TypedAddDbFunction0(fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction1[A:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction2[A:ToDatomicCast, B:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction3[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction4[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction5[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast, E:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)
case class TypedAddDbFunction6[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast, E:ToDatomicCast, F:ToDatomicCast](fn: AddDbFunction) extends TypedAddDbFunction(fn)

object AddDbFunction {
  def apply(kw: Keyword)(params: String*)(lang: String)(code: => String) = new AddDbFunction(kw, lang, params, code)

  def typed(kw: Keyword)(param: String)(lang: String)(code: => String) =
    TypedAddDbFunction0(new AddDbFunction(kw, lang, Seq(param), code))

  def typed[A:ToDatomicCast](kw: Keyword)(param1: String, param2: String)(lang: String)(code: => String) =
    TypedAddDbFunction1[A](new AddDbFunction(kw, lang, Seq(param1, param2), code))

  def typed[A:ToDatomicCast, B:ToDatomicCast](kw: Keyword)(param1: String, param2: String, param3: String)(lang: String)(code: => String) =
    TypedAddDbFunction2[A, B](new AddDbFunction(kw, lang, Seq(param1, param2, param3), code))

  def typed[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast](kw: Keyword)(param1: String, param2: String, param3: String, param4: String)(lang: String)(code: => String) =
    TypedAddDbFunction3[A, B, C](new AddDbFunction(kw, lang, Seq(param1, param2, param3, param4), code))

  def typed[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast](kw: Keyword)(param1: String, param2: String, param3: String, param4: String, param5: String)(lang: String)(code: => String) =
    TypedAddDbFunction4[A, B, C, D](new AddDbFunction(kw, lang, Seq(param1, param2, param3, param4, param5), code))

  def typed[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast, E:ToDatomicCast](kw: Keyword)(param1: String, param2: String, param3: String, param4: String, param5: String, param6: String)(lang: String)(code: => String) =
    TypedAddDbFunction5[A, B, C, D, E](new AddDbFunction(kw, lang, Seq(param1, param2, param3, param4, param5, param6), code))

  def typed[A:ToDatomicCast, B:ToDatomicCast, C:ToDatomicCast, D:ToDatomicCast, E:ToDatomicCast, F:ToDatomicCast](kw: Keyword)(param1: String, param2: String, param3: String, param4: String, param5: String, param6: String, param7: String)(lang: String)(code: => String) =
    TypedAddDbFunction6[A, B, C, D, E, F](new AddDbFunction(kw, lang, Seq(param1, param2, param3, param4, param5, param6, param7), code))
}

class InvokeTxFunction(
    fn:   Keyword,
    args: Seq[DatomicData]
) extends Operation {
  def toNative: AnyRef = {
    datomic.Util.list(
      (fn +: args).map(_.toNative): _*
    )
  }
}

object InvokeTxFunction {
  def apply(fn: Keyword)(args: DatomicData*) = new InvokeTxFunction(fn, args)

  def apply(fn: TypedAddDbFunction0)() =
    new InvokeTxFunction(fn.ident, Seq.empty)

  def apply[A : ToDatomicCast](fn: TypedAddDbFunction1[A])(a: A) =
    new InvokeTxFunction(fn.ident, Seq(Datomic.toDatomic(a)))

  def apply[A : ToDatomicCast,
            B : ToDatomicCast]
           (fn: TypedAddDbFunction2[A, B])
           (a: A, b: B) =
    new InvokeTxFunction(
      fn.ident,
      Seq(
        Datomic.toDatomic(a),
        Datomic.toDatomic(b)))

  def apply[A : ToDatomicCast,
            B : ToDatomicCast,
            C : ToDatomicCast]
           (fn: TypedAddDbFunction3[A, B, C])
           (a: A, b: B, c: C) =
    new InvokeTxFunction(
      fn.ident,
      Seq(
        Datomic.toDatomic(a),
        Datomic.toDatomic(b),
        Datomic.toDatomic(c)))

  def apply[A : ToDatomicCast,
            B : ToDatomicCast,
            C : ToDatomicCast,
            D : ToDatomicCast]
           (fn: TypedAddDbFunction4[A, B, C, D])
           (a: A, b: B, c: C, d: D) =
    new InvokeTxFunction(
      fn.ident,
      Seq(
        Datomic.toDatomic(a),
        Datomic.toDatomic(b),
        Datomic.toDatomic(c),
        Datomic.toDatomic(d)))

  def apply[A : ToDatomicCast,
            B : ToDatomicCast,
            C : ToDatomicCast,
            D : ToDatomicCast,
            E : ToDatomicCast]
           (fn: TypedAddDbFunction5[A, B, C, D, E])
           (a: A, b: B, c: C, d: D, e: E) =
    new InvokeTxFunction(
      fn.ident,
      Seq(
        Datomic.toDatomic(a),
        Datomic.toDatomic(b),
        Datomic.toDatomic(c),
        Datomic.toDatomic(d),
        Datomic.toDatomic(e)))

  def apply[A : ToDatomicCast,
            B : ToDatomicCast,
            C : ToDatomicCast,
            D : ToDatomicCast,
            E : ToDatomicCast,
            F : ToDatomicCast]
           (fn: TypedAddDbFunction6[A, B, C, D, E, F])
           (a: A, b: B, c: C, d: D, e: E, f: F) =
    new InvokeTxFunction(
      fn.ident,
      Seq(
        Datomic.toDatomic(a),
        Datomic.toDatomic(b),
        Datomic.toDatomic(c),
        Datomic.toDatomic(d),
        Datomic.toDatomic(e),
        Datomic.toDatomic(f)))

}

