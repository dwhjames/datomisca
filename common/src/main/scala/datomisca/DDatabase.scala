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

  /**
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t.
    *
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t. Entity ids sort by
    * partition, then T component, such T components interleaving with
    * transaction numbers. Thus this method can be used to fabricate a
    * time-based entity id component for use in e.g. seekDatoms.
    *
    * (Copied from Datomic docs)
    *
    * @param partition
    *     a partition name.
    * @param t
    *     a transaction number, or transaction id.
    * @return a fabricated entity id at or after some point t.
    * @see [[seekDatoms]]
    */
  def entidAt(partition: Partition, t: Long): Long =
    underlying.entidAt(partition.keyword.toNative, t).asInstanceOf[Long]

  /**
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t.
    *
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t. Entity ids sort by
    * partition, then T component, such T components interleaving with
    * transaction numbers. Thus this method can be used to fabricate a
    * time-based entity id component for use in e.g. seekDatoms.
    *
    * (Copied from Datomic docs)
    *
    * @param partition
    *     a partition name.
    * @param t
    *     a point in time.
    * @return a fabricated entity id at or after some point t.
    * @see [[seekDatoms]]
    */
  def entidAt(partition: Partition, t: java.util.Date): Long =
    underlying.entidAt(partition.keyword.toNative, t).asInstanceOf[Long]

  /**
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t.
    *
    * Returns a fabricated entity id in the supplied partition whose
    * T component is at or after the supplied t. Entity ids sort by
    * partition, then T component, such T components interleaving with
    * transaction numbers. Thus this method can be used to fabricate a
    * time-based entity id component for use in e.g. seekDatoms.
    *
    * (Copied from Datomic docs)
    *
    * @param partition
    *     a partition name.
    * @param t
    *     a point in time.
    * @return a fabricated entity id at or after some point t.
    * @see [[seekDatoms]]
    */
  def entidAt(partition: Partition, t: DInstant): Long =
    underlying.entidAt(partition.keyword.toNative, t.underlying).asInstanceOf[Long]

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

    TxReport.toTxReport(javaMap)(this)
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
          filterFn(ddb, new DDatom(d, ddb))
        }
      }
    ))
  }

  def filter(filterFn: DDatom => Boolean): DDatabase = {
    DDatabase(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          filterFn(new DDatom(d, self))
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


  /**
    * Raw access to the index data, by index.
    *
    * Raw access to the index data, by index. The index must be supplied,
    * and, optionally, one or more leading components of the index can be
    * supplied to narrow the result.
    *  - [[DDatabase.EAVT]] and [[DDatabase.AEVT]] indexes will contain all datoms
    *  - [[DDatabase.AVET]] contains datoms for attributes where :db/index = true.
    *  - [[DDatabase.VAET]] contains datoms for attributes of :db.type/ref (it is the reverse index)
    *
    * (Copied from Datomic docs)
    *
    * @param index
    *     the index to use.
    * @param components
    *     optional leading components of the index to match.
    * @return an iterable collection of [[DDatom]].
    */
  def datoms(index: Keyword, components: Keyword*): Iterable[DDatom] =
    new Iterable[DDatom] {
      private val jIterable = underlying.datoms(index.toNative, components.map(_.toNative): _*)
      override def iterator = new Iterator[DDatom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new DDatom(jIter.next(), self)
      }
    }


  /**
    * Raw access to the index data, by index.
    *
    * Raw access to the index data, by index. The index must be supplied,
    * and, optionally, one or more leading components of the index can be
    * supplied for the initial search. Note that, unlike the datoms method,
    * there need not be an exact match on the supplied components. The
    * iteration will begin at or after the point in the index where the
    * components would reside. Further, the iteration is not bound by the
    * supplied components, and will only terminate at the end of the index.
    * Thus you will have to supply your own termination logic, as you rarely
    * want the entire index. As such, seekDatoms is for more advanced
    * applications, and datoms should be preferred wherever it is adequate.
    *  - [[DDatabase.EAVT]] and [[DDatabase.AEVT]] indexes will contain all datoms
    *  - [[DDatabase.AVET]] contains datoms for attributes where :db/index = true.
    *  - [[DDatabase.VAET]] contains datoms for attributes of :db.type/ref (it is the reverse index)
    *
    * (Copied from Datomic docs)
    *
    * @param index
    *     the index to use.
    * @param components
    *     optional leading components of the index to search for.
    * @return an iterable collection of [[DDatom]].
    * @see entidAt
    */
  def seekDatoms(index: Keyword, components: Keyword*): Iterable[DDatom] =
    new Iterable[DDatom] {
      private val jIterable = underlying.seekDatoms(index.toNative, components.map(_.toNative): _*)
      override def iterator = new Iterator[DDatom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new DDatom(jIter.next(), self)
      }
    }


  /**
    * Raw access to the index data for an indexed attribute.
    *
    * Returns a range of datoms in index specified by attrid,
    * starting at start, or from beginning if start is null,
    * and ending before end, or through end of attr index if
    * end is null
    *
    * (Copied from Datomic docs)
    *
    * @param attr
    *     attribute keyword (attribute must be indexed or unique).
    * @param start
    *     some start value or none if beginning.
    * @param end
    *     some end value (non-inclusive), or None if through end.
    * @return an iterable collection of [[DDatom]].
    */
  def indexRange(attr: Keyword, start: Option[AnyRef] = None, end: Option[AnyRef] = None): Iterable[DDatom] =
    new Iterable[DDatom] {
      private val jIterable = underlying.indexRange(attr.toNative, start.orNull, end.orNull)
      override def iterator = new Iterator[DDatom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new DDatom(jIter.next(), self)
      }
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
