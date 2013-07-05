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

import java.{util => ju}


trait QueryExecutor extends QueryExecutorPure with QueryExecutorAuto

/* DATOMIC QUERY */
object QueryExecutor {

  private[datomisca] def directQuery(q: Query, in: Seq[AnyRef]) =
    new Iterable[IndexedSeq[DatomicData]] {
      private val jColl: ju.Collection[ju.List[AnyRef]] = datomic.Peer.q(q.toString, in: _*)
      override def iterator = new Iterator[IndexedSeq[DatomicData]] {
        private val jIter: ju.Iterator[ju.List[AnyRef]] = jColl.iterator
        override def hasNext = jIter.hasNext
        override def next() = new IndexedSeq[DatomicData] {
          private val jList: ju.List[AnyRef] = jIter.next()
          override def length = jList.size
          override def apply(idx: Int): DatomicData =
            DatomicData.toDatomicData(jList.get(idx))
          override def iterator = new Iterator[DatomicData] {
            private val jIter: ju.Iterator[AnyRef] = jList.iterator
            override def hasNext = jIter.hasNext
            override def next() = DatomicData.toDatomicData(jIter.next)
          }
        }
      }
    }

  private[datomisca] def directQueryOut[OutArgs](q: Query, in: Seq[AnyRef])(implicit outConv: QueryResultToTuple[OutArgs]): Iterable[OutArgs] = {
    import scala.collection.JavaConverters._
    new Iterable[OutArgs] {
      private val jColl: ju.Collection[ju.List[AnyRef]] = datomic.Peer.q(q.toString, in: _*)
      override def iterator = new Iterator[OutArgs] {
        private val jIter: ju.Iterator[ju.List[AnyRef]] = jColl.iterator
        override def hasNext = jIter.hasNext
        override def next() = outConv.toTuple(jIter.next())
      }
    }
  }
}

trait QueryExecutorPure {
  def q(query: PureQuery, in: DatomicData*): Iterable[IndexedSeq[DatomicData]] =
    QueryExecutor.directQuery(query, in.map(_.toNative))
}

trait QueryResultToTuple[T] {
  def toTuple(l: ju.List[AnyRef]): T
}

object QueryResultToTuple extends QueryResultToTupleInstances
