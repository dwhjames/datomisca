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


class TxReport(
    val dbBefore: DDatabase,
    val dbAfter:  DDatabase,
    val txData:   Seq[DDatom],
    protected val tempids: AnyRef
) {

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


object TxReport {
  /** Converts a java.util.Map[_,_] returns by connection.transact into a TxReport 
    * It requires an implicit DDatabase because it must resolve the keyword from Datom (from Integer in the map)
    */
  def toTxReport(javaMap: java.util.Map[_, _])(implicit database: DDatabase): TxReport = {
    import scala.collection.JavaConverters._
    import datomic.Connection._
    import datomic.db.Db

    new TxReport(
      dbBefore = DDatabase(
          javaMap.get(DB_BEFORE).asInstanceOf[Db]
        ),
      dbAfter  = DDatabase(
          javaMap.get(DB_AFTER).asInstanceOf[Db]
        ),
      txData =
        javaMap.get(TX_DATA)
               .asInstanceOf[java.util.List[datomic.Datom]]
               .asScala
               .map(new DDatom(_, database))
               .toSeq,
      tempids = javaMap.get(TEMPIDS).asInstanceOf[AnyRef]
    )
  }
}
