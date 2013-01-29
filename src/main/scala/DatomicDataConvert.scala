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

/** DatomicData to Scala reader specific */
trait DD2ScalaReader[-DD <: DatomicData, A] {
  def read(dd: DD): A
}

object DD2ScalaReader extends DD2ScalaReaderImplicits {
  def apply[DD <: DatomicData, A](f: DD => A) = new DD2ScalaReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DD2DDReader[+DD <: DatomicData] {
  def read(d: DatomicData): DD
}

object DD2DDReader extends DD2DDReaderImplicits{
  def apply[DD <: DatomicData](f: DatomicData => DD) = new DD2DDReader[DD]{
    def read(d: DatomicData): DD = f(d)
  }
}

trait DDReader[-DD <: DatomicData, +A] {
  def read(dd: DD): A
}

object DDReader extends DDReaderImplicits{
  def apply[DD <: DatomicData, A](f: DD => A) = new DDReader[DD, A]{
    def read(dd: DD): A = f(dd)
  }
}

trait DDWriter[+DD <: DatomicData, -A] {
  def write(a: A): DD
}

object DDWriter extends DDWriterImplicits {
  def apply[DD <: DatomicData, A](f: A => DD) = new DDWriter[DD, A] {
    def write(a: A) = f(a)
  }
}

trait DD2Writer[-A] {
  def write(a: A): DatomicData
}

object DD2Writer extends DD2WriterImplicits {
  def apply[A](f: A => DatomicData) = new DD2Writer[A] {
    def write(a: A) = f(a)
  }
}
