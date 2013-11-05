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
import java.util.{concurrent => juc}

class TxReportQueue(
    val database: DDatabase,
    val queue:    juc.BlockingQueue[ju.Map[_, _]]
) {


  lazy val stream: Stream[Option[TxReport]] = queue2Stream[ju.Map[_, _]](queue).map{ 
    case None => None
    case Some(javaMap) => Some(TxReport.toTxReport(javaMap)(database))
  }

  private def queue2Stream[A](queue: juc.BlockingQueue[A]): Stream[Option[A]] = {
    def toStream: Stream[Option[A]] = {
      Option(queue.poll()) match {
        case None => Stream.cons(None, toStream)
        case Some(a) => Stream.cons(Some(a), toStream)
      }
    }

    toStream
  }
}
