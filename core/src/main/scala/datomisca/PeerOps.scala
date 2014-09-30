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

import java.util.UUID


/** Regroups most basic [[http://docs.datomic.com/javadoc/datomic/Peer.html datomic.Peer]] functions.
  *
  * The most important is `connect(uri: String)` which builds a [[Connection]]
  * being the entry point for all operations on DB.
  *
  * ''Actually, you need an implicit [[Connection]] in your scope in order to be
  * able to use Datomic Scala facilities.''
  *
  * {{{
  * implicit val conn = Datomic.connection("datomic:mem://mydatabase")
  * }}}
  */
private[datomisca] trait PeerOps {
  /** Builds a Connection from URI
    *
    * In order to benefit from Datomic facilities based on implicit [[Connection]],
    * you should put a connection in an implicit val in your scope.
    * You can also use [[Connection]] explicitly.
    *
    * {{{
    * implicit val conn = Datomic.connection("datomic:mem://mydatabase")
    * }}}
    *
    * @param uri The URI of Datomic DB
    * @return Connection
    */
  def connect(uri: String): Connection =
    new Connection(datomic.Peer.connect(uri))

  /** The database associated to the implicit connection
    * {{{
    * implicit conn = Datomic.connect("datomic:mem://mydatabase")
    * database.transact(...)
    * }}}
    */
  def database()(implicit conn: Connection): Database = conn.database()

  /** Creates a new database using uri
    * @param uri the Uri of the DB
    * @return true/false for success
    */
  def createDatabase(uri: String): Boolean = datomic.Peer.createDatabase(uri)

  /** Deletes an existing database using uri
    * @param uri the URI of the DB
    * @return true/false for success
    */
  def deleteDatabase(uri: String): Boolean = datomic.Peer.deleteDatabase(uri)


  /** Returns the partition of this entity id
    *
    * (Copied from Datomic docs)
    *
    * @param entityId
    * @return the partition of the given entity id
    */
  def part[T](entityId: T)(implicit ev: AsPermanentEntityId[T]): Long =
    datomic.Peer.part(ev.conv(entityId)).asInstanceOf[Long]


  /** Renames an existing database using uri
    * @param uri the URI of the DB
    * @param newName the new name
    * @return true/false for success
    */
  def renameDatabase(uri: String, newName: String): Boolean = datomic.Peer.renameDatabase(uri, newName)

  /** Shutdown all Peer resources.
    * Copied from Datomic Javadoc: This method should be called as part of clean
    * shutdown of a JVM process.
    * Will release all Connections, and, if shutdownClojure is true, will release
    * Clojure resources. Programs written in Clojure can set shutdownClojure to
    * false if they manage Clojure resources (e.g. agents) outside of Datomic;
    * programs written in other JVM languages should typically set shutdownClojure
    * to true.
    */
  def shutdown(shutdownClojure: Boolean): Unit = datomic.Peer.shutdown(shutdownClojure)


  /** Constructs a semi-sequential UUID useful for creating UUIDs that don’t fragment indexes
    *
    * (Copied from Datomic docs)
    *
    * @return a UUID whose most signigicant 32 bits are currentTimeMillis rounded to seconds
    */
  def squuid(): UUID =
    datomic.Peer.squuid()


  /** Get the time part of a squuid
    *
    * (Copied from Datomic docs)
    *
    * @param squuid a UUID created by squuid()
    * @return the time in the format of System.currentTimeMillis
    */
  def squuidTimeMillis(squuid: UUID): Long =
    datomic.Peer.squuidTimeMillis(squuid)


  /** Returns the t value associated with this tx
    *
    * (Copied from Datomic docs)
    *
    * @param tx a transaction entity id
    * @return a database basis T point
    */
  def toT(tx: Long): Long =
    datomic.Peer.toT(tx: java.lang.Long)


  /** Returns the tx associated with this t value.
    *
    * (Copied from Datomic docs)
    *
    * @param t a database basis T point
    * @return a transaction entity id
    */
  def toTx(t: Long): Long =
    datomic.Peer.toTx(t).asInstanceOf[Long]

}
