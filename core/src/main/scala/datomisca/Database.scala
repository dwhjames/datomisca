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


class Database(val underlying: datomic.Database) extends AnyVal {


  /** Returns the entity for the given entity id.
    *
    * This will return an empty [[Entity]], if there
    * are no facts about the given entity id. The
    * :db/id of the entity will be the given id.
    *
    * @param id
    *     an entity id.
    * @return an entity.
    */
  def entity[T](id: T)(implicit ev: AsPermanentEntityId[T]): Entity = {
    val e = underlying.entity(ev.conv(id))
    if (e ne null)
      new Entity(e)
    else
      throw new UnresolvedLookupRefException(ev.conv(id))
  }

  /** Returns the entity for the given keyword.
    *
    * This will return an empty [[Entity]], if there
    * are no facts about the given entity ident. The
    * :db/id of the entity will null.
    *
    * @param kw
    *   a keyword.
    * @return an entity.
    */
  def entity(kw: Keyword): Entity =
    new Entity(underlying.entity(kw))


  /** Returns the value of the database as of some point t, inclusive.
    *
    * @param t
    *     a transactor number, transaction id, or date.
    * @return the value of the database as of some point t, inclusive.
    */
  def asOf[T](t: T)(implicit ev: AsPointT[T]): Database =
    new Database(underlying.asOf(ev.conv(t)))


  /** Returns the value of the database since some point t, exclusive.
    *
    * @param t
    *     a transactor number, or transaction id.
    * @return the value of the database since some point t, exclusive.
    */
  def since[T](t: T)(implicit ev: AsPointT[T]): Database =
    new Database(underlying.since(ev.conv(t)))


  /** Returns the entity id passed.
    *
    * @param id
    *     an entity id.
    * @return the entity id passed.
    */
  def entid[T](id: T)(implicit ev: AsPermanentEntityId[T]): Long = {
    val l = underlying.entid(ev.conv(id)).asInstanceOf[java.lang.Long]
    if (l ne null)
      l.asInstanceOf[Long]
    else
      throw new UnresolvedLookupRefException(ev.conv(id))
  }

  /** Returns the entity id associated with a symbolic keyword
    *
    * @param kw a keyword.
    * @return the entity id.
    * @throws Exception if no entity is found.
    */
  def entid(kw: Keyword): Long =
    Option { underlying.entid(kw) } match {
      case None      => throw new DatomiscaException(s"entity id for keyword $kw not found")
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
    *     a transaction number, transaction id, or date.
    * @return a fabricated entity id at or after some point t.
    * @see [[seekDatoms]]
    */
  def entidAt[T](partition: Partition, t: T)(implicit ev: AsPointT[T]): Long =
    underlying.entidAt(partition.keyword, ev.conv(t)).asInstanceOf[Long]


  /** Returns the symbolic keyword associated with an id
    *
    * @param id an entity id
    * @return a keyword
    * @throws Exception if no keyword is found
    */
  def ident[T](id: T)(implicit ev: AsPermanentEntityId[T]): Keyword =
    Option { underlying.ident(ev.conv(id)) } match {
      case None     => throw new DatomiscaException(s"ident keyword for entity id $id not found")
      case Some(kw) => kw.asInstanceOf[Keyword]
    }

  /** Returns the symbolic keyword
    *
    * @param kw a keyword
    * @return a keyword
    */
  def ident(kw: Keyword): Keyword =
    underlying.ident(kw).asInstanceOf[Keyword]

  /** Applies transaction data to the database
    *
    * It is as if the data was applied in a
    * transaction, but the source of the database
    * is unaffected.
    *
    * @param ops a sequence of tranaction data
    * @return a transaction report
    */
  def withData(ops: Seq[TxData]): TxReport = {
    val javaMap: java.util.Map[_, _] = underlying.`with`(datomic.Util.list(ops.map(_.toTxData): _*))
    new TxReport(javaMap)
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
  def filter(filterFn: (Database, Datom) => Boolean): Database = {
    new Database(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          val ddb = new Database(db)
          filterFn(ddb, new Datom(d))
        }
      }
    ))
  }

  def filter(filterFn: Datom => Boolean): Database = {
    new Database(underlying.filter(
      new datomic.Database.Predicate[datomic.Datom](){
        def apply(db: datomic.Database, d: datomic.Datom): Boolean = {
          filterFn(new Datom(d))
        }
      }
    ))
  }

  /** Combines `entity()` and `entity.touch()`
    *
    * @param id an entity id
    * @return a touched entity
    */
  def touch[T : AsPermanentEntityId](id: T): Entity = entity(id).touch()


  /**
    * Raw access to the index data, by index.
    *
    * Raw access to the index data, by index. The index must be supplied,
    * and, optionally, one or more leading components of the index can be
    * supplied to narrow the result.
    *  - [[Database.EAVT]] and [[Database.AEVT]] indexes will contain all datoms
    *  - [[Database.AVET]] contains datoms for attributes where :db/index = true.
    *  - [[Database.VAET]] contains datoms for attributes of :db.type/ref (it is the reverse index)
    *
    * (Copied from Datomic docs)
    *
    * @param index
    *     the index to use.
    * @param components
    *     optional leading components of the index to match.
    * @return an iterable collection of [[Datom]].
    */
  def datoms(index: Keyword, components: AnyRef*): Iterable[Datom] =
    new Iterable[Datom] {
      private val jIterable = underlying.datoms(index, components: _*)
      override def iterator = new Iterator[Datom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new Datom(jIter.next())
      }
      override def equals(arg: Any) = jIterable.equals(arg)
      override def hashCode = jIterable.hashCode
      override def toString = s"${classOf[Database].getName}.datoms.Iterable@${Integer.toHexString(jIterable.hashCode)}"
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
    *  - [[Database.EAVT]] and [[Database.AEVT]] indexes will contain all datoms
    *  - [[Database.AVET]] contains datoms for attributes where :db/index = true.
    *  - [[Database.VAET]] contains datoms for attributes of :db.type/ref (it is the reverse index)
    *
    * (Copied from Datomic docs)
    *
    * @param index
    *     the index to use.
    * @param components
    *     optional leading components of the index to search for.
    * @return an iterable collection of [[Datom]].
    * @see entidAt
    */
  def seekDatoms(index: Keyword, components: AnyRef*): Iterable[Datom] =
    new Iterable[Datom] {
      private val jIterable = underlying.seekDatoms(index, components: _*)
      override def iterator = new Iterator[Datom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new Datom(jIter.next())
      }
      override def equals(arg: Any) = jIterable.equals(arg)
      override def hashCode = jIterable.hashCode
      override def toString = s"${classOf[Database].getName}.seekDatoms.Iterable@${Integer.toHexString(jIterable.hashCode)}"
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
    * @return an iterable collection of [[Datom]].
    */
  def indexRange(attr: Keyword, start: Option[AnyRef] = None, end: Option[AnyRef] = None): Iterable[Datom] =
    new Iterable[Datom] {
      private val jIterable = underlying.indexRange(attr, start.orNull, end.orNull)
      override def iterator = new Iterator[Datom] {
        private val jIter = jIterable.iterator
        override def hasNext = jIter.hasNext
        override def next() = new Datom(jIter.next())
      }
      override def equals(arg: Any) = jIterable.equals(arg)
      override def hashCode = jIterable.hashCode
      override def toString = s"${classOf[Database].getName}.indexRange.Iterable@${Integer.toHexString(jIterable.hashCode)}"
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
  def history: Database = new Database(underlying.history)

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
  def asOfT: Option[Long] = Option { underlying.asOfT.longValue }

  /**
    * @return the since point
    */
  def sinceT: Option[Long] = Option { underlying.sinceT.longValue }


  /** Look up the database function of the ident entity `ident`, and invoke the function with `args`.
    *
    * The function value is stored as the value of the `:db/fn` attribute of the resolved entity.
    *
    * Note that no casts or conversions are applied to the arguments or the return value.
    *
    * @param  ident  the entity ident.
    * @param  args  the raw arguments to pass to the function.
    * @return the result of the function invocation.
    */
  def invoke(ident: Keyword, args: AnyRef*): AnyRef =
    underlying.invoke(entid(ident), args:_*)


  /** Look up the database function of the entity `id`, and invoke the function with `args`.
    *
    * The function value is stored as the value of the `:db/fn` attribute of the resolved entity.
    *
    * Note that no casts or conversions are applied to the arguments or the return value.
    *
    * @param  id  the entity id.
    * @param  args  the raw arguments to pass to the function.
    * @return the result of the function invocation.
    */
  def invoke[T](id: T, args: AnyRef*)(implicit ev: AsPermanentEntityId[T]): AnyRef =
    underlying.invoke(ev.conv(id), args:_*)

}

object Database {
  // Index component contains all datoms
  val EAVT = datomic.Database.EAVT.asInstanceOf[Keyword]
  // Index component contains all datoms
  val AEVT = datomic.Database.AEVT.asInstanceOf[Keyword]
  // Index component contains datoms for attributes where :db/index = true
  val AVET = datomic.Database.AVET.asInstanceOf[Keyword]
  // Index component contains datoms for attributes of :db.type/ref
  val VAET = datomic.Database.VAET.asInstanceOf[Keyword]
} 
