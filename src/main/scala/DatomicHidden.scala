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

trait DatomicQueryHidden {
  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args1, OutArgs], d1: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF1[List[T]]).execute(d1)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args2, OutArgs], d1: DatomicData, d2: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF2[List[T]]).execute(d1, d2)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args3, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF3[List[T]]).execute(d1, d2, d3)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args4, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF4[List[T]]).execute(d1, d2, d3, d4)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args5, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF5[List[T]]).execute(d1, d2, d3, d4, d5)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args6, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF6[List[T]]).execute(d1, d2, d3, d4, d5, d6)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args7, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF7[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args8, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF8[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args9, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF9[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args10, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF10[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args11, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF11[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args12, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF12[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args13, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF13[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args14, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF14[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args15, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF15[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15)

  def q[OutArgs <: Args, T](q: TypedQueryInOut[Args16, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF16[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args17, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF17[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args18, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF18[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args19, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF19[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args20, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF20[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args21, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData, d21: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF21[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20, d21)

 def q[OutArgs <: Args, T](q: TypedQueryInOut[Args22, OutArgs], d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData, d21: DatomicData, d22: DatomicData)(
    implicit outConv: DatomicDataToArgs[OutArgs], ott: ArgsToTuple[OutArgs, T]
  ) = q.prepare[T]()(outConv, ott, ArgsImplicits.toF22[List[T]]).execute(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20, d21, d22)

}

trait ToFunctionImplicitsHidden {

  implicit def toF1[Out] = new ToFunction[Args1, Out] {
    type F[Out] = Function1[DatomicData, Out]
    def convert(f: (Args1 => Out)): F[Out] = (d1: DatomicData) => f(Args1(d1))
  }

  implicit def toF2[Out] = new ToFunction[Args2, Out] {
    type F[Out] = Function2[DatomicData, DatomicData, Out]
    def convert(f: (Args2 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData) => f(Args2(d1, d2)) 
  }

  implicit def toF3[Out] = new ToFunction[Args3, Out] {
    type F[Out] = Function3[DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args3 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData) => f(Args3(d1, d2, d3))
  }

  implicit def toF4[Out] = new ToFunction[Args4, Out] {
    type F[Out] = Function4[DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args4 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData) => f(Args4(d1, d2, d3, d4))
  }

  implicit def toF5[Out] = new ToFunction[Args5, Out] {
    type F[Out] = Function5[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args5 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData) => f(Args5(d1, d2, d3, d4, d5))
  }

  implicit def toF6[Out] = new ToFunction[Args6, Out] {
    type F[Out] = Function6[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args6 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData) => f(Args6(d1, d2, d3, d4, d5, d6))
  }

  implicit def toF7[Out] = new ToFunction[Args7, Out] {
    type F[Out] = Function7[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args7 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData) => f(Args7(d1, d2, d3, d4, d5, d6, d7))
  }

  implicit def toF8[Out] = new ToFunction[Args8, Out] {
    type F[Out] = Function8[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args8 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData) => f(Args8(d1, d2, d3, d4, d5, d6, d7, d8))
  }

  implicit def toF9[Out] = new ToFunction[Args9, Out] {
    type F[Out] = Function9[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args9 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData) => f(Args9(d1, d2, d3, d4, d5, d6, d7, d8, d9))
  }

  implicit def toF10[Out] = new ToFunction[Args10, Out] {
    type F[Out] = Function10[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args10 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData) => f(Args10(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10))
  }

  implicit def toF11[Out] = new ToFunction[Args11, Out] {
    type F[Out] = Function11[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args11 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData) => f(Args11(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11))
  }

  implicit def toF12[Out] = new ToFunction[Args12, Out] {
    type F[Out] = Function12[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args12 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData) => f(Args12(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12))
  }

  implicit def toF13[Out] = new ToFunction[Args13, Out] {
    type F[Out] = Function13[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args13 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData) => f(Args13(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13))
  }

  implicit def toF14[Out] = new ToFunction[Args14, Out] {
    type F[Out] = Function14[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args14 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData) => f(Args14(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14))
  }

  implicit def toF15[Out] = new ToFunction[Args15, Out] {
    type F[Out] = Function15[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args15 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData) => f(Args15(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15))
  }

  implicit def toF16[Out] = new ToFunction[Args16, Out] {
    type F[Out] = Function16[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args16 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData) => f(Args16(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16))
  }

  implicit def toF17[Out] = new ToFunction[Args17, Out] {
    type F[Out] = Function17[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args17 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData) => f(Args17(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17))
  }

  implicit def toF18[Out] = new ToFunction[Args18, Out] {
    type F[Out] = Function18[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args18 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData) => f(Args18(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18))
  }

  implicit def toF19[Out] = new ToFunction[Args19, Out] {
    type F[Out] = Function19[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args19 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData) => f(Args19(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19))
  }

  implicit def toF20[Out] = new ToFunction[Args20, Out] {
    type F[Out] = Function20[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args20 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData) => f(Args20(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20))
  }

  implicit def toF21[Out] = new ToFunction[Args21, Out] {
    type F[Out] = Function21[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args21 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData, d21: DatomicData) => f(Args21(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20, d21))
  }

  implicit def toF22[Out] = new ToFunction[Args22, Out] {
    type F[Out] = Function22[DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, Out]
    def convert(f: (Args22 => Out)): F[Out] = (d1: DatomicData, d2: DatomicData, d3: DatomicData, d4: DatomicData, d5: DatomicData, d6: DatomicData, d7: DatomicData, d8: DatomicData, d9: DatomicData, d10: DatomicData, d11: DatomicData, d12: DatomicData, d13: DatomicData, d14: DatomicData, d15: DatomicData, d16: DatomicData, d17: DatomicData, d18: DatomicData, d19: DatomicData, d20: DatomicData, d21: DatomicData, d22: DatomicData) => f(Args22(d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20, d21, d22))
  }
}

trait DatomicDataToArgsImplicitsHidden {
  implicit object DatomicDataToTuple1 extends DatomicDataToArgs[DatomicData] {
    def toArgs(l: Seq[DatomicData]): DatomicData = l match {
      case List(_1) => _1
      case _ => throw new RuntimeException("Could convert Seq to DatomicData")
    }
  }

  implicit object DatomicDataToTuple2 extends DatomicDataToArgs[(DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData) = l match {
      case List(_1, _2) => (_1, _2)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple3 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3) => (_1, _2, _3)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple4 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4) => (_1, _2, _3, _4)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple5 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5) => (_1, _2, _3, _4, _5)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple6 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5, _6) => (_1, _2, _3, _4, _5, _6)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple7 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5, _6, _7) => (_1, _2, _3, _4, _5, _6, _7)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple8 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8) => (_1, _2, _3, _4, _5, _6, _7, _8)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple9 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9) => (_1, _2, _3, _4, _5, _6, _7, _8, _9)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToTuple10 extends DatomicDataToArgs[(DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def toArgs(l: Seq[DatomicData]): (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData) = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10) => (_1, _2, _3, _4, _5, _6, _7, _8, _9, _10)
      case _ => throw new RuntimeException("Could convert Seq to (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)")
    }
  }

  implicit object DatomicDataToArgs2 extends DatomicDataToArgs[Args2] {
    def toArgs(l: Seq[DatomicData]): Args2 = l match {
      case List(_1, _2) => Args2(_1, _2)
      case _ => throw new RuntimeException("Could convert Seq to Args2")
    }
  }

  implicit def DatomicDataToArgs3 = new DatomicDataToArgs[Args3] {
    def toArgs(l: Seq[DatomicData]): Args3 = l match {
      case List(_1, _2, _3) => Args3(_1, _2, _3)
      case _ => throw new RuntimeException("Could convert Seq to Args3")
    }
  }

  implicit def DatomicDataToArgs4 = new DatomicDataToArgs[Args4] {
    def toArgs(l: Seq[DatomicData]): Args4 = l match {
      case List(_1, _2, _3, _4) => Args4(_1, _2, _3, _4)
      case _ => throw new RuntimeException("Could convert Seq to Args4")
    }
  }

  implicit def DatomicDataToArgs5 = new DatomicDataToArgs[Args5] {
    def toArgs(l: Seq[DatomicData]): Args5 = l match {
      case List(_1, _2, _3, _4, _5) => Args5(_1, _2, _3, _4, _5)
      case _ => throw new RuntimeException("Could convert Seq to Args5")
    }
  }

  implicit def DatomicDataToArgs6 = new DatomicDataToArgs[Args6] {
    def toArgs(l: Seq[DatomicData]): Args6 = l match {
      case List(_1, _2, _3, _4, _5, _6) => Args6(_1, _2, _3, _4, _5, _6)
      case _ => throw new RuntimeException("Could convert Seq to Args6")
    }
  }

  implicit def DatomicDataToArgs7 = new DatomicDataToArgs[Args7] {
    def toArgs(l: Seq[DatomicData]): Args7 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7) => Args7(_1, _2, _3, _4, _5, _6, _7)
      case _ => throw new RuntimeException("Could convert Seq to Args7")
    }
  }

  implicit def DatomicDataToArgs8 = new DatomicDataToArgs[Args8] {
    def toArgs(l: Seq[DatomicData]): Args8 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8) => Args8(_1, _2, _3, _4, _5, _6, _7, _8)
      case _ => throw new RuntimeException("Could convert Seq to Args8")
    }
  }

  implicit def DatomicDataToArgs9 = new DatomicDataToArgs[Args9] {
    def toArgs(l: Seq[DatomicData]): Args9 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9) => Args9(_1, _2, _3, _4, _5, _6, _7, _8, _9)
      case _ => throw new RuntimeException("Could convert Seq to Args9")
    }
  }

  implicit def DatomicDataToArgs10 = new DatomicDataToArgs[Args10] {
    def toArgs(l: Seq[DatomicData]): Args10 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10) => Args10(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10)
      case _ => throw new RuntimeException("Could convert Seq to Args10")
    }
  }

  implicit def DatomicDataToArgs11 = new DatomicDataToArgs[Args11] {
    def toArgs(l: Seq[DatomicData]): Args11 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11) => Args11(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11)
      case _ => throw new RuntimeException("Could convert Seq to Args11")
    }
  }

  implicit def DatomicDataToArgs12 = new DatomicDataToArgs[Args12] {
    def toArgs(l: Seq[DatomicData]): Args12 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12) => Args12(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12)
      case _ => throw new RuntimeException("Could convert Seq to Args12")
    }
  }

  implicit def DatomicDataToArgs13 = new DatomicDataToArgs[Args13] {
    def toArgs(l: Seq[DatomicData]): Args13 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13) => Args13(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13)
      case _ => throw new RuntimeException("Could convert Seq to Args13")
    }
  }

  implicit def DatomicDataToArgs14 = new DatomicDataToArgs[Args14] {
    def toArgs(l: Seq[DatomicData]): Args14 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14) => Args14(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14)
      case _ => throw new RuntimeException("Could convert Seq to Args14")
    }
  }

  implicit def DatomicDataToArgs15 = new DatomicDataToArgs[Args15] {
    def toArgs(l: Seq[DatomicData]): Args15 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15) => Args15(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15)
      case _ => throw new RuntimeException("Could convert Seq to Args15")
    }
  }

  implicit def DatomicDataToArgs16 = new DatomicDataToArgs[Args16] {
    def toArgs(l: Seq[DatomicData]): Args16 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16) => Args16(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16)
      case _ => throw new RuntimeException("Could convert Seq to Args16")
    }
  }

  implicit def DatomicDataToArgs17 = new DatomicDataToArgs[Args17] {
    def toArgs(l: Seq[DatomicData]): Args17 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17) => Args17(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17)
      case _ => throw new RuntimeException("Could convert Seq to Args17")
    }
  }

  implicit def DatomicDataToArgs18 = new DatomicDataToArgs[Args18] {
    def toArgs(l: Seq[DatomicData]): Args18 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18) => Args18(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18)
      case _ => throw new RuntimeException("Could convert Seq to Args18")
    }
  }

  implicit def DatomicDataToArgs19 = new DatomicDataToArgs[Args19] {
    def toArgs(l: Seq[DatomicData]): Args19 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19) => Args19(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19)
      case _ => throw new RuntimeException("Could convert Seq to Args19")
    }
  }

  implicit def DatomicDataToArgs20 = new DatomicDataToArgs[Args20] {
    def toArgs(l: Seq[DatomicData]): Args20 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20) => Args20(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20)
      case _ => throw new RuntimeException("Could convert Seq to Args20")
    }
  }

  implicit def DatomicDataToArgs21 = new DatomicDataToArgs[Args21] {
    def toArgs(l: Seq[DatomicData]): Args21 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21) => Args21(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21)
      case _ => throw new RuntimeException("Could convert Seq to Args21")
    }
  }

  implicit def DatomicDataToArgs22 = new DatomicDataToArgs[Args22] {
    def toArgs(l: Seq[DatomicData]): Args22 = l match {
      case List(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21, _22) => Args22(_1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _20, _21, _22)
      case _ => throw new RuntimeException("Could convert Seq to Args22")
    }
  }
}

trait ArgsToTupleImplicitsHidden {
  implicit def Args2ToTuple = new ArgsToTuple[Args2, (DatomicData, DatomicData)] {
    def convert(from: Args2) = (from._1, from._2)
  }

  implicit def Args3ToTuple = new ArgsToTuple[Args3, (DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args3) = (from._1, from._2, from._3)
  }

  implicit def Args4ToTuple = new ArgsToTuple[Args4, (DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args4) = (from._1, from._2, from._3, from._4)
  }

  implicit def Args5ToTuple = new ArgsToTuple[Args5, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args5) = (from._1, from._2, from._3, from._4, from._5)
  }

  implicit def Args6ToTuple = new ArgsToTuple[Args6, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args6) = (from._1, from._2, from._3, from._4, from._5, from._6)
  }

  implicit def Args7ToTuple = new ArgsToTuple[Args7, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args7) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7)
  }

  implicit def Args8ToTuple = new ArgsToTuple[Args8, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args8) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8)
  }

  implicit def Args9ToTuple = new ArgsToTuple[Args9, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args9) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9)
  }

  implicit def Args10ToTuple = new ArgsToTuple[Args10, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args10) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10)
  }

  implicit def Args11ToTuple = new ArgsToTuple[Args11, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args11) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11)
  }

  implicit def Args12ToTuple = new ArgsToTuple[Args12, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args12) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12)
  }

  implicit def Args13ToTuple = new ArgsToTuple[Args13, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args13) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13)
  }

  implicit def Args14ToTuple = new ArgsToTuple[Args14, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args14) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14)
  }

  implicit def Args15ToTuple = new ArgsToTuple[Args15, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args15) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15)
  }

  implicit def Args16ToTuple = new ArgsToTuple[Args16, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args16) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16)
  }

  implicit def Args17ToTuple = new ArgsToTuple[Args17, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args17) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17)
  }

  implicit def Args18ToTuple = new ArgsToTuple[Args18, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args18) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17, from._18)
  }

  implicit def Args19ToTuple = new ArgsToTuple[Args19, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args19) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17, from._18, from._19)
  }

  implicit def Args20ToTuple = new ArgsToTuple[Args20, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args20) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17, from._18, from._19, from._20)
  }

  implicit def Args21ToTuple = new ArgsToTuple[Args21, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args21) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17, from._18, from._19, from._20, from._21)
  }

  implicit def Args22ToTuple = new ArgsToTuple[Args22, (DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData, DatomicData)] {
    def convert(from: Args22) = (from._1, from._2, from._3, from._4, from._5, from._6, from._7, from._8, from._9, from._10, from._11, from._12, from._13, from._14, from._15, from._16, from._17, from._18, from._19, from._20, from._21, from._22)
  }
}

