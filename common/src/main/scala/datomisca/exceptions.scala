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


class DatomicException(msg: String) extends Exception(msg)

class EntityNotFoundException(id: String)
  extends DatomicException(s"Datomic Error: entity not found with id($id)")

class TempidNotResolved(id: DId)
  extends DatomicException(s"Datomic Error: entity not found with id($id)")

class UnexpectedDatomicTypeException(typeName: String)
  extends DatomicException(s"Datomic Error: unresolved datomic type $typeName")

class EntityKeyNotFoundException(keyword: String)
  extends DatomicException(s"The keyword $keyword not found in the entity")

class EntityMappingException(msg: String)
  extends DatomicException(s"Datomic Error: $msg")
