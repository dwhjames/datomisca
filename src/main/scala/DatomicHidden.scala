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

