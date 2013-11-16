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


object PlutoSampleData extends SampleData {

  override val schema = Datomic.parseOps("""
    {:db/id #db/id [:db.part/db]
     :db/ident :object/name
     :db/doc "Name of a Solar System object."
     :db/valueType :db.type/string
     :db/index true
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id [:db.part/db]
     :db/ident :object/meanRadius
     :db/doc "Mean radius of an object."
     :db/index true
     :db/valueType :db.type/double
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id [:db.part/db]
     :db/ident :data/source
     :db/doc "Source of the data in a transaction."
     :db/valueType :db.type/string
     :db/index true
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
  """).get

  override val txData = Datomic.parseOps("""
    {:db/id #db/id [:db.part/tx]
     :db/doc "Solar system objects bigger than Pluto."}
    {:db/id #db/id [:db.part/tx]
     :data/source "http://en.wikipedia.org/wiki/List_of_Solar_System_objects_by_size"}
    {:db/id #db/id [:db.part/user]
     :object/name "Sun"
     :object/meanRadius 696000.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Jupiter"
     :object/meanRadius 69911.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Saturn"
     :object/meanRadius 58232.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Uranus"
     :object/meanRadius 25362.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Neptune"
     :object/meanRadius 24622.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Earth"
     :object/meanRadius 6371.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Venus"
     :object/meanRadius 6051.8}
    {:db/id #db/id [:db.part/user]
     :object/name "Mars"
     :object/meanRadius 3390.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Ganymede"
     :object/meanRadius 2631.2}
    {:db/id #db/id [:db.part/user]
     :object/name "Titan"
     :object/meanRadius 2576.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Mercury"
     :object/meanRadius 2439.7}
    {:db/id #db/id [:db.part/user]
     :object/name "Callisto"
     :object/meanRadius 2410.3}
    {:db/id #db/id [:db.part/user]
     :object/name "Io"
     :object/meanRadius 1821.5}
    {:db/id #db/id [:db.part/user]
     :object/name "Moon"
     :object/meanRadius 1737.1}
    {:db/id #db/id [:db.part/user]
     :object/name "Europa"
     :object/meanRadius 1561.0}
    {:db/id #db/id [:db.part/user]
     :object/name "Triton"
     :object/meanRadius 1353.4}
    {:db/id #db/id [:db.part/user]
     :object/name "Eris"
     :object/meanRadius 1163.0}
  """).get
}
