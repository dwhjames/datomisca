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


trait Attribute2PartialAddEntityWriter[DD <: DatomicData, Card <: Cardinality, Dest] {
  def convert(attr: Attribute[DD, Card]): PartialAddEntityWriter[Dest]
}

object Attribute2PartialAddEntityWriter {

  implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData, Source](implicit tdat: ToDatomic[DD, Source]) =
    new Attribute2PartialAddEntityWriter[DD, CardinalityOne.type, Source] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): PartialAddEntityWriter[Source] = {
        PartialAddEntityWriter[Source]{ s: Source =>
          PartialAddEntity( Map( attr.ident -> tdat.to(s) ) )
        }
      }
    }  


  implicit def attr2PartialAddEntityWriterMany[DD <: DatomicData, Source](implicit tdat: ToDatomic[DColl, Set[Source]]) =
    new Attribute2PartialAddEntityWriter[DD, CardinalityMany.type, Set[Source]] {
      def convert(attr: Attribute[DD, CardinalityMany.type]): PartialAddEntityWriter[Set[Source]] = {
        PartialAddEntityWriter[Set[Source]]{ s: Set[Source] =>
          if (s.isEmpty) PartialAddEntity( Map.empty )
          else PartialAddEntity( Map( attr.ident -> tdat.to(s) ) )
        }
      }
    }

  /*implicit def attr2PartialAddEntityWriterOne[DD <: DatomicData] = 
    new Attribute2PartialAddEntityWriter[DD, CardinalityOne.type, DD] {
      def convert(attr: Attribute[DD, CardinalityOne.type]): PartialAddEntityWriter[DD] = {
        PartialAddEntityWriter[DD]{ d: DD => 
          PartialAddEntity( Map( attr.ident -> d ) )
        }
      }
    }*/

}
