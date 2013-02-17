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

class DDatabase(val underlying: datomic.Database) extends DatomicData {
  self => 

  /** Returns the entity for the given entity id
    *
    * @param eid an entity id
    * @return an entity
    * @throws EntityNotFoundException if there is no such entity
    */
  def entity(eid: Long): DEntity =
    wrapEntity(eid.toString, underlying.entity(eid))

  def entity(e: DLong):   DEntity = entity(e.underlying)
  def entity(e: FinalId): DEntity = entity(e.underlying)

  /** Returns the entity for the given keyword
    *
    * @param kw a keyword
    * @return an entity
    * @throws EntityNotFoundException if there is no such entity
    */
  def entity(kw: Keyword): DEntity =
    wrapEntity(kw.toString, underlying.entity(kw.toNative))

  private def wrapEntity(id: String, entity: datomic.Entity): DEntity =
    if (entity.keySet.isEmpty)
      throw new EntityNotFoundException(id)
    else
      DEntity(entity)


  /**
    * @param date a Date
    * @return the value of the database as of some date
    */
  def asOf(date: java.util.Date): DDatabase = DDatabase(underlying.asOf(date))
  def asOf(date: DInstant):       DDatabase = asOf(date.underlying)

  /**
    * @param date a Date
    * @return the value of the database since some date
    */
  def since(date: java.util.Date): DDatabase = DDatabase(underlying.since(date))
  def since(date: DInstant): DDatabase = since(date.underlying)

  /**
    * @param eid an entity id
    * @return the entity id
    * @throws Exception if no entity is found
    */
  def entid(eid: Long): Long =
    Option { underlying.entid(eid) } match {
      case None      => throw new Exception(s"DDatabase.entid: entity id $eid not found")
      case Some(eid) => eid.asInstanceOf[Long]
    }

  def entid(eid: DLong): Long = entid(eid.underlying)

  /** Returns the entity id associated with a symbolic keyword
    *
    * @param kw a keyword
    * @return the entity id
    * @throws Exception if no entity is found
    */
  def entid(kw: Keyword): Long =
    Option { underlying.entid(kw.toNative) } match {
      case None      => throw new Exception(s"DDatabase.entid: entity id for keyword $kw not found")
      case Some(eid) => eid.asInstanceOf[Long]
    }

  /** Returns the symbolic keyword associated with an id
    *
    * @param eid an entity id
    * @return a keyword
    * @throws Exception if no keyword is found
    */
  def ident(eid: Long): Keyword =
    Option { underlying.ident(eid) } match {
      case None     => throw new Exception("DDatabase.ident: keyword not found")
      case Some(kw) => Keyword(kw.asInstanceOf[clojure.lang.Keyword])
    }

  /** Returns the symbolic keyword
    *
    * @param kw a keyword
    * @return a keyword
    */
  def ident(kw: Keyword): Keyword =
    Keyword(underlying.ident(kw.toNative).asInstanceOf[clojure.lang.Keyword])

  /** Applies transaction data to the database
    *
    * It is as if the data was applied in a
    * transaction, but the source of the database
    * is unaffected.
    *
    * @param ops a sequence of tranaction data
    * @return a transaction report
    */
  def withData(ops: Seq[Operation]): TxReport = {
    import scala.collection.JavaConverters._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val javaMap: java.util.Map[_, _] = underlying.`with`(datomicOps)

    Utils.toTxReport(javaMap)(this)
  }

  /** Returns the value of the database containing only Datoms
    * satisfying the predicate.
    *
    * The predicate will be passed the unfiltered db and a Datom.
    * Chained calls compose the predicate with 'and'.
    *
    * @param filterFn a predicate
    * @return the value of the database satisfying the predicate
    */
  def filter(filterFn: (DDatabase, DDatom) => Boolean): DDatabase = {
    DDatabase(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          val ddb = DDatabase(db)
          filterFn(ddb, DDatom(d)(ddb))
        }
      }
    ))
  }

  def filter(filterFn: DDatom => Boolean): DDatabase = {
    DDatabase(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          filterFn(DDatom(d)(self))
        }
      }
    ))
  }

  /** Combines `entity()` and `entity.touch()`
    *
    * @param eid an entity id
    * @return a touched entity
    */
  def touch(eid: Long):  DEntity = entity(eid).touch()
  def touch(eid: DLong): DEntity = entity(eid).touch()

  def datoms(index: Keyword, components: Keyword*): Seq[DDatom] = {
    //import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._
    underlying.datoms(index.toNative, components.map(_.toNative): _*).toSeq.map( d => DDatom(d)(this) )
  }

  /** Returns a special database containing all assertions
    * and retractions across time.
    *
    * This special database can be used for `datoms()` and
    * `indexRange()` calls and queries, but not for `entity()`
    * or `with()` calls. `asOf()` and `since()` bounds are also
    * supported. Note that queries will get all of the
    * additions and retractions, which can be distinguished
    * by the fifth datom field 'added'
    * (true for add/assert) `[e a v tx added]`
    */
  def history = DDatabase(underlying.history)
  

  /**
    * @return the database id
    */
  def id: String = underlying.id

  /**
    * @return true if db has had a filter set with filter(filterFn)
    */
  def isFiltered: Boolean = underlying.isFiltered

  /**
    * @return true if this is a special history db
    */
  def isHistory: Boolean = underlying.isHistory

  /**
    * @return the t of the most recent transaction reachable via this db value
    */
  def basisT: Long = underlying.basisT

  /**
    * the t one beyond the highest reachable via this db value
    */
  def nextT: Long = underlying.nextT

  /**
    * @return the asOf point
    */
  def asOfT: Option[Long] = Option { underlying.asOfT }

  /**
    * @return the since point
    */
  def sinceT: Option[Long] = Option { underlying.sinceT }

  // TODO
  // indexRange
  // invoke

  override def toString = underlying.toString
  def toNative: AnyRef = underlying
}

object DDatabase {
  def apply(underlying: datomic.Database) = new DDatabase(underlying)

  // Index component contains all datoms
  val EAVT = Keyword("eavt")
  // Index component contains all datoms
  val AEVT = Keyword("aevt")
  // Index component contains datoms for attributes where :db/index = true
  val AVET = Keyword("avet")
  // Index component contains datoms for attributes of :db.type/ref
  val VAET = Keyword("vaet")
} 
