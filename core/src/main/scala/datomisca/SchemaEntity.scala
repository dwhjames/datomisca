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

import java.{util => ju}


object SchemaEntity {

  class SchemaEntityBuilder {

    private val builder = Map.newBuilder[Keyword, AnyRef]

    def +=[DD <: AnyRef, Card <: Cardinality, T]
          (attrVal: (Attribute[DD, Card], T))
          (implicit ev: Attribute2FactWriter[DD, Card, T])
          : this.type = {
      builder += ev.convert(attrVal)
      this
    }

    def +?=[DD <: AnyRef, Card <: Cardinality, T]
          (attrVal: (Attribute[DD, Card], Option[T]))
          (implicit ev: Attribute2FactWriter[DD, Card, T])
          : this.type = {
      for (t <- attrVal._2) {
        builder += ev.convert(attrVal._1, t)
      }
      this
    }

    def ++=[DD <: AnyRef, Card <: Cardinality, Coll, T]
          (attrVal: (Attribute[DD, Card], Coll))
          (implicit ev1: Coll <:< Traversable[T], ev2: Attribute2FactWriter[DD, Card, T])
          : this.type = {
      val attr = attrVal._1
      val coll = attrVal._2
      val arrayList: ju.ArrayList[AnyRef] = ev1(coll) match {
          case t: Iterable[_] => new ju.ArrayList[AnyRef](t.size)
          case _ => new ju.ArrayList[AnyRef]()
        }
      for (t <- coll) {
        arrayList add ev2.convert(attr, t)._2
      }
      builder += (attr.ident -> arrayList)
      this
    }

    def ++=(partial: PartialAddEntity): this.type = {
      builder ++= partial.props
      this
    }

    def partial(): PartialAddEntity =
      new PartialAddEntity(builder.result())

    def withId[T](id: T)(implicit ev: AsEntityId[T]): AddEntity =
      new AddEntity(ev.conv(id), builder.result)
  }

  def newBuilder: SchemaEntityBuilder = new SchemaEntityBuilder
}
