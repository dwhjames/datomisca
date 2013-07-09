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
  def connect(uri: String): Connection = {
    val conn = datomic.Peer.connect(uri)

    Connection(conn)
  }

  /** The database associated to the implicit connection
    * {{{
    * implicit conn = Datomic.connect("datomic:mem://mydatabase")
    * database.transact(...)
    * }}}
    */
  def database(implicit conn: Connection) = conn.database

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
}
