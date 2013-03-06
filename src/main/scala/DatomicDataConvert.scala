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

 trait DatomicDataReader[A] {
 	def read(dd: DatomicData): A
 }

 object DatomicDataReader extends DatomicDataReaderImplicits {
 	def apply[A](f: DatomicData => A) = new DatomicDataReader[A] {
 		def read(dd: DatomicData): A = f(dd)
 	}
 }

/** DatomicData to Scala reader specific */
trait DDReader[DD <: DatomicData, A] {
  def read(dd: DD): A
}

object DDReader extends DDReaderImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new DDReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DatomicDataWriter[A] {
  def write(a: A): DatomicData
}

object DatomicDataWriter extends DatomicDataWriterImplicits{
  def apply[A](f: A => DatomicData) = new DatomicDataWriter[A] {
    def write(a: A): DatomicData = f(a)
  }
}

trait DDWriterCore[DD <: DatomicData, A] {
  def write(a: A): DD
}

object DDWriterCore extends DDWriterCoreImplicits {
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriterCore[DD, A] {
    def write(a: A) = f(a)
  }
}

trait DDWriter[DD <: DatomicData, A] {
  def write(a: A): DD
}

object DDWriter extends DDWriterImplicits{
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriter[DD, A] {
    def write(a: A) = f(a)
  }
}

