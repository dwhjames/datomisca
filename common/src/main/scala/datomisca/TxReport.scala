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


class TxReport(rawReport: java.util.Map[_, _]) {
  import scala.collection.JavaConverters._
  import datomic.Connection.{DB_BEFORE, DB_AFTER, TX_DATA, TEMPIDS}
  import datomic.db.Db

  def dbBefore: DDatabase =
    DDatabase(rawReport.get(DB_BEFORE).asInstanceOf[Db])

  def dbAfter: DDatabase =
    DDatabase(rawReport.get(DB_AFTER).asInstanceOf[Db])

  lazy val txData: Seq[DDatom] = {
    val builder = Seq.newBuilder[DDatom]
    val iter = rawReport.get(TX_DATA).asInstanceOf[java.util.List[datomic.Datom]].iterator
    while (iter.hasNext) {
      builder += new DDatom(iter.next())
    }
    builder.result()
  }

  private val tempids = rawReport.get(TEMPIDS).asInstanceOf[AnyRef]

  def resolve(id: DId): Long =
    resolveOpt(id) getOrElse { throw new TempidNotResolved(id) }

  def resolve(identified: TempIdentified): Long =
    resolve(identified.id)

  def resolve(ids: DId*): Seq[Long] =
    ids map { resolve(_) }

  def resolveOpt(id: DId): Option[Long] =
    Option {
      datomic.Peer.resolveTempid(dbAfter.underlying, tempids, id.toNative)
    } map { id =>
      id.asInstanceOf[Long]
    }

  def resolveOpt(ids: DId*): Seq[Option[Long]] =
    ids map { resolveOpt(_) }

  def resolveEntity(id: DId): DEntity =
    dbAfter.entity(resolve(id))

  lazy val tempidMap = new Map[DId, Long] {
    override def get(tempId: DId) = resolveOpt(tempId)
    override def iterator = throw new UnsupportedOperationException
    override def +[T >: Long](kv: (DId, T)) = throw new UnsupportedOperationException
    override def -(k: DId) = throw new UnsupportedOperationException
  }

  override def toString = s"""TxReport{
    dbBefore: ${dbBefore.basisT},
    dbAfter: ${dbAfter.basisT},
    txData: $txData,
    tempids: $tempids
  }"""
}
