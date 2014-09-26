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

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try


object SchemaManager {

  def hasAttribute(attributeIdent: Keyword)(implicit db: Database): Boolean =
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

  private[datomisca] def hasSchema(schemaTag: Keyword, schemaName: String)(implicit db: Database): Boolean =
    ! Datomic.q(schemaTagQuery, db, schemaTag, schemaName).isEmpty

  private[datomisca] def ensureSchemaTag(schemaTag: Keyword)(implicit conn: Connection, ec: ExecutionContext): Future[Boolean] =
    Future {
      hasAttribute(schemaTag)(conn.database())
    } flatMap {
      case true  => Future.successful(false)
      case false =>
        Datomic.transact(
          Attribute(schemaTag, SchemaType.string, Cardinality.one)
            .withDoc("Name of schema installed by this transaction")
            .withIndex(true)
        ) map { _ => true }
    }

  private[datomisca] def ensureSchemas(
      schemaTag:   Keyword,
      schemaMap:   Map[String, (Seq[String], Seq[Seq[TxData]])],
      schemaNames: String*)
     (implicit conn: Connection, ec: ExecutionContext)
     : Future[Boolean] = {
    Future.traverse(schemaNames) { schemaName =>
      val (requires, txDatas) = schemaMap(schemaName)
      ensureSchemas(schemaTag, schemaMap, requires: _*) flatMap { dependentsChanged =>
        if (txDatas.isEmpty) {
          throw new DatomiscaException(s"No schema data provided for schema ${schemaName}")
        } else if (hasSchema(schemaTag, schemaName)(conn.database())) {
          Future.successful(dependentsChanged)
        } else {
          Future.traverse(txDatas) { txData =>
            Datomic.transact(
              txData :+
              // NOTE: this means that if multiple transactions are
              // required for this schema fragment, then each
              // transaction is tagged.
              Fact.add(DId(Partition.TX))(schemaTag -> schemaName)
            )
          } map { _ => true }
        }
      }
    } map { s => s.exists(identity) }
  }

  def installSchema(
      schemaTag:   Keyword,
      schemaMap:   Map[String, (Seq[String], Seq[Seq[TxData]])],
      schemaNames: String*)
     (implicit conn: Connection, ec: ExecutionContext)
     : Future[Boolean] =
    for {
      tag <- ensureSchemaTag(schemaTag)
      schema <- ensureSchemas(schemaTag, schemaMap, schemaNames: _*)
    } yield ( tag || schema )

}
