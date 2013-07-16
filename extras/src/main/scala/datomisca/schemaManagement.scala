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

import scala.language.reflectiveCalls

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try


object SchemaManager {

  def hasAttribute(attributeIdent: Keyword)(implicit db: DDatabase): Boolean =
    Try {
      db.entity(attributeIdent)
    } .map { entity =>
      entity(Namespace.DB.INSTALL / "_attribute")
    } .isSuccess

  private val schemaTagQuery = Query("""
      [:find ?e
       :in $ ?schemaTag ?schemaName
       :where [?e ?schemaTag ?schemaName]]
    """)

  private[datomisca] def hasSchema(schemaTag: Keyword, schemaName: String)(implicit db: DDatabase): Boolean =
    ! Datomic.q(schemaTagQuery, db, DKeyword(schemaTag), DString(schemaName)).isEmpty

  private[datomisca] def ensureSchemaTag(schemaTag: Keyword)(implicit conn: Connection): Future[Unit] =
    future {
      hasAttribute(schemaTag)(conn.database)
    } flatMap {
      case true  => Future.successful(())
      case false =>
        Datomic.transact(
          Attribute(schemaTag, SchemaType.string, Cardinality.one)
            .withDoc("Name of schema installed by this transaction")
            .withIndex(true)
        ) map { _ => () }
    }

  private[datomisca] def ensureSchemas(
      schemaTag:   Keyword,
      schemaMap:   Map[String, (Seq[String], Seq[Seq[Operation]])],
      schemaNames: String*)
     (implicit conn: Connection)
     : Future[Unit] = {
    Future.traverse(schemaNames) { schemaName =>
      future {
        hasSchema(schemaTag, schemaName)(conn.database)
      } flatMap {
        case true  =>
          Future.successful(())
        case false =>
          val (requires, txDatas) = schemaMap(schemaName)
          ensureSchemas(schemaTag, schemaMap, requires: _*) flatMap { _ =>
            if (txDatas.isEmpty) {
              throw new RuntimeException(s"DatomicSchemaManager.ensureSchemas: no data provided for schema ${schemaName}")
            } else {
              Future.traverse(txDatas) { txData =>
                Datomic.transact(
                  txData :+
                  Fact.add(DId(Partition.TX))(schemaTag -> schemaName)
                ) map { _ => () }
              }
            }
          }
      }
    } map { _ => () }
  }

  def installSchema(
      schemaTag:   Keyword,
      schemaMap:   Map[String, (Seq[String], Seq[Seq[Operation]])],
      schemaNames: String*)
     (implicit conn: Connection)
     : Future[Unit] =
    for {
      _ <- ensureSchemaTag(schemaTag)
      _ <- ensureSchemas(schemaTag, schemaMap, schemaNames: _*)
    } yield ()

}
