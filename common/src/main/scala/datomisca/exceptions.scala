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


class DatomiscaException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) {
    this(message, null)
  }
}

class TempidNotResolved(id: DId)
  extends DatomiscaException(s"entity not found with id($id)")

class UnsupportedDatomicTypeException(cls: Class[_])
  extends DatomiscaException(s"Datomic returned un supported ${cls.getName}")

class EntityKeyNotFoundException(keyword: String)
  extends DatomiscaException(s"the keyword $keyword not found in the entity map")

class EntityMappingException(message: String)
  extends DatomiscaException(message)
