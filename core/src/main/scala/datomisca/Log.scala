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

import java.util.{Date => JDate}

/** Datomic's database log is a recording of all transaction data in historic
  * order, organized for efficient access by transaction.
  *
  * @param log
  *   the underlying log.
  * @see [[http://docs.datomic.com/log.html Log API]]
  */
class Log(val log: datomic.Log) {

  /** A transaction in the log
    *
    * A transaction consists of the T point of the transaction, along with the
    * collection of [[DDatom]]s asserts or retracted by the transaction.
    */
  trait Tx {

    /** The T point of the transaction. */
    val t: Long

    /** An iterable of the [[DDatom]]s asserted or retracted by the transaction. */
    val datoms: Iterable[DDatom]
  }
  /** Returns a range of transactions in log.
    *
    * Returns a range of transactions in log, starting at startT, or from
    * beginning if start is None, and ending before end, or through end of log
    * if end is None.
    *
    * (Copied from Datomic docs.)
    *
    * @param startT
    *     Some transaction number, transaction ID, Date or None.
    * @param endT
    *     Some transaction number, transaction ID, Date or None.
    * @return an Iterable of transactions occurring between start (inclusive) and end (exclusive).
    */
  def txRange[T1, T2](
      startT: Option[T1],
      endT:   Option[T2]
  )(implicit ev1: AsPointT[T1],
             ev2: AsPointT[T2]
  ): Iterable[Tx] = new Iterable[Tx] {
    import scala.collection.JavaConverters._
    private val jIterable = log.txRange(startT.map(ev1.conv).orNull, endT.map(ev2.conv).orNull)
    override def iterator = new Iterator[Tx] {
      private val jIter = jIterable.iterator
      override def hasNext = jIter.hasNext
      override def next() = new Tx {
        private val javaMap: java.util.Map[_, _] = jIter.next()
        override val t = javaMap.get(datomic.Log.T).asInstanceOf[Long]
        override val datoms = new Iterable[DDatom] {
          private val jIterableDatoms =
            javaMap.get(datomic.Log.DATA)
                   .asInstanceOf[java.lang.Iterable[datomic.Datom]]
          override def iterator = new Iterator[DDatom] {
            private val jIterDatoms = jIterableDatoms.iterator
            override def hasNext = jIterDatoms.hasNext
            override def next() = new DDatom(jIterDatoms.next())
          }
        }
      }
    }
  }

}
