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

/** Generic DatomicData to Scala type 
  * Multi-valued "function" (not real function actually) 
  * which inverse is surjective DDWriterEpi or DDWriterMulti
  * 1 DatomicData -> n Scala type
  */
trait DDReaderMulti[A] {
	def read(dd: DatomicData): A
}

object DDReaderMulti extends DDReaderMultiImplicits {
	def apply[A](f: DatomicData => A) = new DDReaderMulti[A] {
		def read(dd: DatomicData): A = f(dd)
	}
}

/** Specific DatomicData to Scala reader monomorphic (injective)
  * 1 DD => 1 Scala type
  */
trait DDReaderMono[DD <: DatomicData, A] {
  def read(dd: DD): A
}

object DDReaderMono extends DDReaderMonoImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new DDReaderMono[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

/** Scala type to Generic DatomicData (surjective)
  * n Scala type -> DatomicData
  */
trait DDWriterMulti[A] {
  def write(a: A): DatomicData
}

object DDWriterMulti extends DDWriterMultiImplicits {
  def apply[A](f: A => DatomicData) = new DDWriterMulti[A] {
    def write(a: A): DatomicData = f(a)
  }
}

/** Scala to Specific DatomicData to Scala writer monomorphic (injective)
  * 1 Scala type => 1 DD
  */
trait DDWriterMono[DD <: DatomicData, A] {
  def write(a: A): DD
}

object DDWriterMono extends DDWriterMonoImplicits {
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriterMono[DD, A] {
    def write(a: A) = f(a)
  }
}

/** Scala to Specific DatomicData to Scala writer epimorphic (surjective)
  * n Scala type => 1 DD
  */
trait DDWriterEpi[DD <: DatomicData, A] {
  def write(a: A): DD
}

object DDWriterEpi extends DDWriterEpiImplicits{
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriterEpi[DD, A] {
    def write(a: A) = f(a)
  }
}

trait DDIso[DD <: DatomicData, A] extends DDReaderMono[DD, A] with DDWriterMono[DD, A]

trait DDMulti[A] extends DDReaderMulti[A] with DDWriterMulti[A]
