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

}
