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


sealed trait Props {
  def convert: PartialAddEntity

  private def ::[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): Props = PropsLink(prop, this, attrC)

  def +[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]) = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => if(head._1 == prop._1) (prop :: tail)(attrC) else (head :: step(tail))(ac)
        case PropsNil => prop :: PropsNil
      }
    }

    step(this)
  }

  def -[DD <: DatomicData, Card <: Cardinality](attr: Attribute[DD, Card]) = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => if(head._1 == attr) tail else (head :: step(tail))(ac)
        case PropsNil => PropsNil
      }
    }

    step(this)
  }

  def ++(other: Props): Props = {
    def step(cur: Props): Props = {
      cur match {
        case PropsLink(head, tail, ac) => (head :: step(tail))(ac)
        case PropsNil => other
      }
    }

    step(this)
  }
}

object Props {
  def apply() = PropsNil

  def apply[DD <: DatomicData, Card <: Cardinality, A](prop: (Attribute[DD, Card], A))  
    (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A]): Props = {
      prop :: PropsNil
  }
}

case object PropsNil extends Props {
  def convert: PartialAddEntity = PartialAddEntity.empty
}

case class PropsLink[DD <: DatomicData, Card <: Cardinality, A](
  head: (Attribute[DD, Card], A), 
  tail: Props, 
  attrC: Attribute2PartialAddEntityWriter[DD, Card, A]
) extends Props {
  override def toString = s"""${head._1.ident} -> ${head._2} :: $tail""" 

  def convert: PartialAddEntity = {
    attrC.convert(head._1).write(head._2) ++ tail.convert
  }
}
