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

import java.io.Reader
import java.io.FileReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import scala.reflect.macros.Context
import scala.reflect.internal.util.{Position, OffsetPosition}
import scala.tools.reflect.Eval
import language.experimental.macros

import dmacros._


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
trait DatomicPeer {
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
  implicit def database(implicit conn: Connection) = conn.database

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
}


/** Provides all Datomic Database Transactor asynchronous function.
  *
  * Please note that those functions require :
  *   - an implicit [[Connection]] for transaction,
  *   - Scala [[scala.concurrent.ExecutionContext]] for Future management
  */
trait DatomicTransactor {

  /** Performs an Datomic async transaction with multiple operations.
    *
    * {{{
    * Datomic.transact(Seq(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * )).map{ tx =>
    *     ...
    * }
    * }}}
    *
    * @param ops a sequence of [[Operation]]
    * @param connection the implicit [[Connection]]
    * @param ex the implicit [[scala.concurrent.ExecutionContext]]
    * @return A future of Transaction Report
    *
    */
  def transact(ops: Seq[Operation])(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = connection.transact(ops)

  /** Performs an Datomic async transaction with single operation.
    *
    * {{{
    * Datomic.transact(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   )
    * )).map{ tx =>
    *     ...
    * }
    * }}}
    * @param op the [[Operation]]
    * @param connection the implicit [[Connection]]
    * @param ex the implicit [[scala.concurrent.ExecutionContext]]
    * @return A future of Transaction Report
    */
  def transact(op: Operation)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op))

  /** Performs an Datomic async transaction with multiple operations.
    *
    * {{{
    * Datomic.transact(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "age" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * ).map{ tx =>
    *     ...
    * }
    * }}}
    *
    * @param op 1st [[Operation]]
    * @param ops Other [[Operation]]s
    * @param connection the implicit [[Connection]]
    * @param ex the implicit [[scala.concurrent.ExecutionContext]]
    * @return A future of Transaction Report
    */
  def transact(op: Operation, ops: Operation*)(implicit connection: Connection, ex: ExecutionContext): Future[TxReport] = transact(Seq(op) ++ ops)  

  /** Applies a sequence of operations to current database without applying the transaction.
    * 
    * It's is as if the data was applied in a transaction but the database is unaffected.
    * This is the same as Java `database.with(...)` taking into account `with` is a reserved word in Scala.
    *
    * Please note the result is not asynchronous Future as the operations is applied at the Peer level
    *
    * {{{
    * Datomic.withData(
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "toto",
    *     person / "agreede" -> 30L
    *   ),
    *   AddToEntity(DId(Partition.USER))(
    *     person / "name" -> "tata",
    *     person / "age" -> 54L
    *   )
    * ).map{ tx =>
    *   // Peer database contains the known data + toto + tata
    * }
    * }}}
    *
    */
  def withData(ops: Seq[Operation])(implicit connection: Connection): TxReport = connection.database.withData(ops)
}

trait DatomicTypeWrapper {
  import scala.language.implicitConversions

  /** implicit converters to simplify conversion from Scala Types to Datomic Type */
  implicit def toDWrapper[T](t: T)(implicit td: ToDatomicCast[T]): DWrapper = DWrapperImpl(Datomic.toDatomic(t)(td))

  trait DWrapper extends NotNull
  private[datomisca] case class DWrapperImpl(underlying: DatomicData) extends DWrapper

}

/** Provides all Datomic Scala specific facilities
  */
trait DatomicFacilities extends DatomicTypeWrapper{
  
  /** Converts any value to a DatomicData given there is the right [[DDWriter]] in the scope
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val s: DString = Datomic.toDatomic("toto")
    * val l: DLong = Datomic.toDatomic("5L")
    * }}}
    */
  def toDatomic[T](t: T)(implicit tdc: ToDatomicCast[T]): DatomicData = tdc.to(t)
  
  /** converts a DatomicData to a type given there is the right [[DDReader]] in the scope 
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val l: String = Datomic.fromDatomic(DString("toto"))
    * val s: Long = Datomic.fromDatomic(DLong(5L))
    * }}}
    */
  def fromDatomic[DD <: DatomicData, T](dd: DD)(implicit fd: FromDatomicInj[DD, T]): T = fd.from(dd)

  /** Converts any data to a Datomic Data (or not if not possible) */
  def toDatomicData(v: Any): DatomicData = v match {
    case s: String => DString(s)
    case b: Boolean => DBoolean(b)
    case i: Int => DLong(i)
    case l: Long => DLong(l)
    case f: Float => DFloat(f)
    case d: Double => DDouble(d)
    case bi: BigInt => DBigInt(bi)
    case bd: BigDecimal => DBigDec(bd)
    case bi: java.math.BigInteger => DBigInt(BigInt(bi))
    case bd: java.math.BigDecimal => DBigDec(BigDecimal(bd))
    case d: java.util.Date => DInstant(d)
    case u: java.util.UUID => DUuid(u)
    case u: java.net.URI => DUri(u)
    case kw: clojure.lang.Keyword => DRef(Keyword(kw.getName, Namespace(kw.getNamespace)))
    case e: datomic.Entity => DEntity(e)
    case coll: java.util.Collection[_] => 
      import scala.collection.JavaConversions._
      DSet(coll.toSet.map{dd: Any => toDatomicData(dd)})

    //DRef(DId(e.get(Keyword("id", Namespace.DB).toNative).asInstanceOf[Long]))
    // REF???
    case v => throw new UnexpectedDatomicTypeException(v.getClass.getName)
  }

  /** Macro-based helper to create Datomic keyword using Clojure-style
    * {{{val kw = KW(":person/name")}}}
    *
    * @param q the Clojure string
    * @return parsed [[Keyword]]
    */
  def KW(q: String): Keyword = macro DatomicQueryMacro.KWImpl

  /** Helper: creates a [[DSet]] from simple types using DWrapper implicit conversion
    *
    * {{{
    * val addPartOp = Datomic.set("toto", 3L, "tata")
    * }}}
    *
    * @param partition the partition to create
    */
  def set(dw: DWrapper*) = DSet(dw.map{t: DWrapper => t.asInstanceOf[DWrapperImpl].underlying}.toSet)

  /** Runtime-based helper to create multiple Datomic Operations (Add, Retract, RetractEntity, AddToEntity) 
    * compiled from a Clojure String. '''This is not a Macro so no variable in string and it is evaluated
    * at runtime'''
    *
    * You can then directly copy some Clojure code in a String and get it parsed at runtime. This is why
    * it returns a `Try[Seq[Operation]]`
    * It also manages comments.
    *
    * {{{
    * val ops = Datomic.parseOps("""
    * ;; comment blabla
    * [
    *   [:db/add #db/id[:db.part/user] :db/ident :character/weak]
    *   ;; comment blabla
    *   [:db/add #db/id[:db.part/user] :db/ident :character/dumb]
    *   [:db/add #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retract #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retractEntity 1234]
    *   ;; comment blabla
    *   {
    *     :db/id #db/id[:db.part/user]
    *     :person/name "toto, tata"
    *     :person/age 30
    *     :person/character [ :character/_weak :character/dumb-toto ]
    *   }
    *   { :db/id #db/id[:db.part/user], :person/name "toto",
    *     :person/age 30, :person/character [ :character/_weak, :character/dumb-toto ]
    *   }
    * ]""")
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations or an error
    */
  def parseOps(q: String): Try[Seq[Operation]] = DatomicParser.parseOpSafe(q) match {
    case Left(PositionFailure(msg, offsetLine, offsetCol)) =>
      Failure(new RuntimeException(s"Couldn't parse operations[msg:$msg, line:$offsetLine, col:$offsetCol]"))
    case Right(ops) => 
      Success(ops)
  }

  /** Macro-based helper to create multiple Datomic Operations (Add, Retract, RetractEntity, AddToEntity) 
    * compiled from a Clojure String extended with Scala variables.
    *
    * You can then directly copy some Clojure code in a String and get it compiled.
    * You can also use variables in this String in String interpolation style.
    *
    * {{{
    * val id = DId(Partition.USER)
    *
    * val weak = AddIdent(Keyword(Namespace("person.character"), "weak"))
    * val dumb = AddIdent(Keyword(Namespace("person.character"), "dumb"))
    *
    * val id = DId(Partition.USER)
    * val ops = Datomic.ops("""[
    *   [:db/add #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/add \${DId(Partition.USER)} :db/ident :region/n]
    *   [:db/retract #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retractEntity 1234]
    *   {
    *     :db/id \${id}
    *     :person/name "toto"
    *     :person/age 30
    *     :person/character [ \$weak \$dumb ]
    *   }
    * ]""")
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations
    */
  def ops(q: String): Seq[Operation] = macro DatomicMacroOps.opsImpl
}


/** Main object containing:
  *    - all Datomic basic functions (Peer, Transactor)
  *    - all Scala basic functions
  *    - all Scala high-level functions (macro, typed ops) 
  *    - all implicit DDReader/DDWriter
  *
  *
  * {{{
  * import Datomic._ // brings all DDReader/DDWriter
  * }}}
  */
object Datomic 
  extends DatomicPeer 
  with DatomicTransactor 
  with DatomicFacilities 
  with DatomicQueryExecutor 
  with DatomicTypeWrapper

class DatomicException(msg: String) extends Exception(msg)

class EntityNotFoundException(id: String)
  extends DatomicException(s"Datomic Error: entity not found with id($id)")

class TempidNotResolved(id: DId)
  extends DatomicException(s"Datomic Error: entity not found with id($id)")

class UnexpectedDatomicTypeException(typeName: String)
  extends DatomicException(s"Datomic Error: unresolved datomic type $typeName")

class EntityKeyNotFoundException(keyword: Keyword)
  extends DatomicException(s"The keyword $keyword not found in the entity")

class EntityMappingException(msg: String)
  extends DatomicException(s"Datomic Error: $msg")
