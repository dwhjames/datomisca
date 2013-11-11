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


object SchemaEntity {
  /** AddEntity based on Schema attributes 
    */
  def add[T](id: T)(props: Props)(implicit ev: AsEntityId[T]): AddEntity =
    new AddEntity(ev.conv(id), props.convert.props)

  class SchemaEntityBuilder {

    private val builder = Map.newBuilder[Keyword, DatomicData]

    def +=[DD <: DatomicData, Card <: Cardinality, A]
          (attrVal: (Attribute[DD, Card], A))
          (implicit attrC: Attribute2PartialAddEntityWriter[DD, Card, A])
          : this.type = {
      builder ++= attrC.convert(attrVal._1).write(attrVal._2).props.toTraversable
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
