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

  def entity(e: DLong): DEntity = entity(e.underlying)
  def entity(e: FinalId): DEntity = entity(e.underlying)
  def entity(e: Long): DEntity = Option(underlying.entity(e)) match {
    case None => throw new EntityNotFoundException(DId(e))
    case Some(entity) => 
      if(entity.keySet.isEmpty) throw new EntityNotFoundException(DId(e))
      else DEntity(entity)
  }

  def asOf(date: java.util.Date): DDatabase = DDatabase(underlying.asOf(date))
  def asOf(date: DInstant): DDatabase = asOf(date.underlying)

  def since(date: java.util.Date): DDatabase = DDatabase(underlying.since(date))
  def since(date: DInstant): DDatabase = since(date.underlying)

  def entid(e: Long):     Long = underlying.entid(e).asInstanceOf[Long]
  def entid(e: DLong):    Long = entid(e.underlying)
  def entid(kw: Keyword): Long = underlying.entid(kw.toNative).asInstanceOf[Long]

  def ident(e: Integer): Keyword = Keyword(underlying.ident(e).asInstanceOf[clojure.lang.Keyword])
  def ident(kw: Keyword): Keyword = Keyword(underlying.ident(kw.toNative).asInstanceOf[clojure.lang.Keyword])

  def withData(ops: Seq[Operation]) = {
    import scala.collection.JavaConverters._

    val datomicOps = ops.map( _.toNative ).toList.asJava

    val javaMap: java.util.Map[_, _] = underlying.`with`(datomicOps)

    Utils.toTxReport(javaMap)(this)
  }

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

  def touch(id: Long): DEntity = touch(DLong(id))
  def touch(id: DLong): DEntity = touch(entity(id))
  def touch(entity: DEntity): DEntity = entity.touch

  def datoms(index: Keyword, components: Keyword*): Seq[DDatom] = {
    //import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._
    underlying.datoms(index.toNative, components.map(_.toNative): _*).toSeq.map( d => DDatom(d)(this) )
  }

  def history = DDatabase(underlying.history)
  

  def id: String = underlying.id
  def isFiltered: Boolean = underlying.isFiltered
  def isHistory: Boolean = underlying.isHistory
  def basisT: Long = underlying.basisT
  def nextT: Long = underlying.nextT
  def sinceT: Option[Long] = Option(underlying.sinceT)

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
