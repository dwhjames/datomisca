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

package reactivedatomic

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

/** Provides all Datomic Scala specific facilities
  *
  */
trait DatomicFacilities {
  /** implicit converters to simplify conversion from Scala Types to Datomic Types
    */
  implicit def toDWrapper[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DWrapper = DWrapperImpl(toDatomic(t)(ddw))

  /** Creates a single Add operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    *
    * {{{
    * val totoName = Datomic.add(DId(Partition.USER))( person / "name" -> "toto")
    * }}}
    * 
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: DId)(prop: (Keyword, DWrapper)) = Add(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Add operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.add(DLong(3L))( person / "name" -> "toto")
    * }}}
    * 
    * @param id a DLong corresponding to a real [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: DLong)(prop: (Keyword, DWrapper)) = Add(DId(id), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Add operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/add entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.add(3L)( person / "name" -> "toto")
    * }}}
    * 
    * @param id a Long corresponding to a real [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def add(id: Long)(prop: (Keyword, DWrapper)) = Add(DId(DLong(id)), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Retract operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.retract(DId(Partition.USER))( person / "name" -> "toto")
    * }}}
    * 
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: DId)(prop: (Keyword, DWrapper)) = Retract(id, prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Retract operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.retract(DLong(3L))( person / "name" -> "toto")
    * }}}
    * 
    * @param id the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: DLong)(prop: (Keyword, DWrapper)) = Retract(DId(id), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single Retract operation targeting a given [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db/retract entity-id attribute value]}}}
    * 
    * {{{
    * val totoName = Datomic.retract(3L)( person / "name" -> "toto")
    * }}}
    * 
    * @param id the [[Long]] of the targeted [[DId]]
    * @param prop a tuple ([[Keyword]], value)<br/>
    *             where value can be any String/Long/Double/Float/Boolean/Date/BigInt/BigDec/DRef 
    *             converted to [[DatomicData]] using [[toDWrapper]] implicit conversion
    */
  def retract(id: Long)(prop: (Keyword, DWrapper)) = Retract(DId(DLong(id)), prop._1, prop._2.asInstanceOf[DWrapperImpl].underlying)

  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    * 
    * {{{
    * val retractEntity = Datomic.retractEntity(DLong(3L))
    * }}}
    * 
    * @param id the DLong of a targeted real [[DId]]
    */
  def retractEntity(id: DLong) = RetractEntity(id)

  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    * 
    * {{{
    * val retractEntity = Datomic.retractEntity(3L)
    * }}}
    * 
    * @param id the long of a targeted real [[DId]]
    */
  def retractEntity(id: Long) = RetractEntity(DLong(id))

  /** Creates a single RetractEntity operation targeting a real [[DId]] (can't be a temporary Id)
    * 
    * In Clojure, this is equivalent to:
    * {{{[:db.fn/retractEntity entity-id]}}}
    * 
    * {{{
    * val retractEntity = Datomic.retractEntity(DId(3L))
    * }}}
    * 
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    */
  def retractEntity(id: FinalId) = RetractEntity(DLong(id.underlying))

  /** Creates a Multiple-"Add" targeting a single [[DId]]
    * 
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ... 
    * }
    * }}}
    * 
    * {{{
    * val toto = Datomic.addToEntity(DId(Partition.USER))(
    *   person / "name" -> "toto",
    *   person / "age" -> 30L
    * )
    * }}}
    * 
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    */
  def addToEntity(id: DId)(props: (Keyword, DWrapper)*) = 
    AddToEntity(id)(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ): _*)

  /** Creates a Multiple-"Add" targeting a single [[DId]] from a simple Map[[[Keyword]], [[DatomicData]]]
    * 
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ... 
    * }
    * }}}
    * 
    * {{{
    * val toto = Datomic.addToEntity(DId(Partition.USER), Map(
    *   person / "name" -> DString("toto"),
    *   person / "age" -> DLong(30L)
    * ))
    * }}}
    * 
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param props the map containing all tupled (keyword, value)
    */
    def addToEntity(id: DId, props: Map[Keyword, DatomicData]) = AddToEntity(id, props)


  /** Creates a Multiple-"Add" targeting a single [[DId]] and using a [[PartialAddToEntity]]
    * which is basically a AddToEntity without the DId part (''technical API'').
    * 
    * In Clojure, this is equivalent to:
    * {{{
    * {:db/id entity-id
    *  attribute value
    *  attribute value
    *  ... 
    * }
    * }}}
    * 
    * @param id the targeted [[DId]] which must be a [[FinalId]]
    * @param props a PartialAddToEntity containing tuples (keyword, value)
    */
  def addToEntity(id: DId, props: PartialAddToEntity) = AddToEntity(id, props)

  def partialAddToEntity(props: (Keyword, DWrapper)*) = 
    PartialAddToEntity(props.map( t => (t._1, t._2.asInstanceOf[DWrapperImpl].underlying) ).toMap)

  /** Helper: creates a special AddToEntity for creating a new Partition
    *
    * {{{
    * val addPartOp = Datomic.addPartition(Partition(Namespace.DB.PART / "mypart"))
    * }}}
    *
    * @param partition the partition to create
    */
  def addPartition(partition: Partition) = 
    AddToEntity(DId(Partition.DB))(
      Namespace.DB / "ident" -> DString(partition.toString),
      Namespace.DB.INSTALL / "_partition" -> DString("db.part/db")
    )

  /** Helper: creates a [[DSet]] from simple types using DWrapper implicit conversion
    *
    * {{{
    * val addPartOp = DSet("toto", 3L, "tata")
    * }}}
    *
    * @param partition the partition to create
    */
  def dset(dw: DWrapper*) = DSet(dw.map{t: DWrapper => t.asInstanceOf[DWrapperImpl].underlying}.toSet)

  /** Converts any value to a DatomicData given there is the right [[DDWriter]] in the scope
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val s: DString = Datomic.toDatomic("toto")
    * val l: DLong = Datomic.toDatomic("5L")
    * }}}
    */
  def toDatomic[T](t: T)(implicit ddw: DDWriter[DatomicData, T]): DatomicData = ddw.write(t)
  
  /** converts a DatomicData to a type given there is the right [[DDReader]] in the scope 
    *
    * {{{
    * import Datomic._ // brings all DDReader/DDWriter
    * val l: String = Datomic.fromDatomic(DString("toto"))
    * val s: Long = Datomic.fromDatomic(DLong(5L))
    * }}}
    */
  def fromDatomic[T] = new {
    def apply[DD <: DatomicData](dd: DD)(implicit ddr: DDReader[DD, T]): T = ddr.read(dd)
  }

}

trait DatomicTyped {

  /** a subobject containing all Compile-time "Typed" facilities for:
    *    - Compile-time Typed Queries
    *    - Compile-time Typed Entities based on typed schema attributes
    */
  object typed extends DatomicSchemaFacilities {
    /** Creates a macro-based compile-time typed query from a String:
      *    - syntax validation is performed.
      *    - type validation (for the time being, number of input/output args, later much more)
      *
      * '''Keep in mind a query is an immutable data structure that you can manipulate'''
      * 
      * A ``TypedQuery[InArgs, OutArgs] takes 2 type parameters:
      *     - InArgs <: Args defining the number of input args
      *     - OutArgs <: args defining the number of output args
      * 
      * When a [[TypedQuery]] is executed, it returns a `List[TupleX[DatomicData, DatomicData, ...]]` where X corresponds
      * to the number of output parameters
      *
      * {{{
      * val q = Datomic.typedQuery[Args2, Args3](""" 
      *   [
      *    :find ?e ?name ?age
      *    :in $ [[?name ?age]]
      *    :where [?e :person/name ?name]
      *           [?e :person/age ?age]
      *   ]
      * """)
      *
      * Datomic.query(
      *   q, database, 
      *   DSet(
      *     DSet(DString("toto"), DLong(30L)),
      *     DSet(DString("tutu"), DLong(54L))
      *   )
      * ).map{
      *   case (e: DLong, n: DString, a: DLong) => 
      *      ...
      * }    
      * }}}
      */
    def query[A <: Args, B <: Args](q: String): TypedQuery[A, B] = macro DatomicQueryMacro.typedQueryImpl[A, B]    
  }

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
  with DatomicDataImplicits 
  with ArgsImplicits 
  with DatomicQuery 
  with DatomicTyped {

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
    case v => throw new RuntimeException("Unknown Datomic underlying "+v.getClass)
  }

  /** Creates a macro-based compile-time pure query from a String (only syntax validation is performed).<br/>
    * '''Keep in mind a query is an immutable data structure that you can manipulate'''
    * 
    *     - A [[PureQuery]] is the low-level query AST provided in the Scala API.
    *     - [[TypedQuery]] is based on it.
    *     - When a [[PureQuery]] is executed, it returns a `List[List[DatomicData]].
    *     - All returned types are [[DatomicData]].
    * 
    * {{{
    * // creates a query
    * val q = pureQuery("""
    *  [ 
    *    :find ?e ?name
    *    :in $ ?char
    *    :where  [ ?e :person/name ?name ] 
    *            [ ?e :person/character ?char ]
    *  ]
    * """)
    *
    * Datomic.query(q, database, person.character / "violent").map{
    *   case List(e: DLong, name: DString) =>
    *     ...
    * }
    * }}}
    *
    * @param q the query String
    * @return a PureQuery
    */
  def pureQuery(q: String): PureQuery = macro DatomicQueryMacro.pureQueryImpl

  
  //def typedQuery[A <: Args, B <: Args](q: String): TypedQuery[A, B] = macro DatomicQueryMacro.typedQueryImpl[A, B]

  /** Macro-based helper to create Rule alias to be used in Queries.
    * {{{
    * val totoRule = Datomic.rules("""
    *   [[[toto ?e]
    *     [?e :person/name "toto"]
    *   ]]
    * """)
    *
    * val q = Datomic.typedQuery[Args2, Args2]("""
    *   [
    *     :find ?e ?age
    *     :in $ %
    *     :where [?e :person/age ?age]
    *            (toto ?e)
    *   ]
    * """)
    *
    * Datomic.query(q, database, totoRule).map {
    *   case (e: DLong, age: DLong) => 
    *     ...
    * }
    * }}}
    */
  def rules(q: String): DRuleAliases = macro DatomicQueryMacro.rulesImpl

  /** Macro-based helper to create Datomic keyword using Clojure-style
    * {{{val kw = KW(":person/name")}}}
    *
    * @param q the Clojure string
    * @return parsed [[Keyword]]
    */
  def KW(q: String): Keyword = macro DatomicQueryMacro.KWImpl

  /** Macro-based helper to create Datomic AddToEntity compiled from a Clojure String extended with Scala variables.
    *
    * You can then directly copy some Clojure code in a String and get it compiled.
    * You can also use variables in this String in String interpolation style.
    *
    * {{{
    * val name = "toto"
    * val weak = AddIdent(Keyword(person.character, "weak"))
    * val dumb = AddIdent(Keyword(person.character, "dumb"))
    *
    * Datomic.addToEntity("""{
    *   :db/id ${DId(Partition.USER)}
    *   :person/name $name
    *   :person/age 30
    *   :person/character [ $weak $dumb ]
    * }""")
    * }}}
    *
    * @param q the Clojure string
    * @return the operation
    */
  def addToEntity(q: String): AddToEntity = macro DatomicMacroOps.addToEntityImpl

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
    *   [:db/add ${DId(Partition.USER)} :db/ident :region/n]
    *   [:db/retract #db/id[:db.part/user] :db/ident :region/n]
    *   [:db/retractEntity 1234]
    *   {
    *     :db/id ${id}
    *     :person/name "toto"
    *     :person/age 30
    *     :person/character [ $weak $dumb ]
    *   }
    * ]""")
    * }}}
    *
    * @param q the Clojure string
    * @return a sequence of operations
    */
  def ops(q: String): Seq[Operation] = macro DatomicMacroOps.opsImpl

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
}


