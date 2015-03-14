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

import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.concurrent.duration._

import java.{util => ju}
import java.util.{concurrent => juc}


/** The data queue associated with a connection.
  *
  * At any point in time either zero or one queue is associated with
  * a connection.
  *
  * This queue may be safely consumed from more than one thread.
  * Note that the queue does not block producers, and will consume
  * memory until you consume the elements from it.

  * Reports will be added to the queue at some point after the
  * database has been updated.
  * If the associated connection originated the transaction, the
  * transaction future will be notified first, before a report is
  * placed on this queue.
  *
  * @param  queue  the underlying transaction report queue.
  * @see [[http://docs.datomic.com/javadoc/datomic/Connection.html#txReportQueue() datomic.Connection.txReportQueue()]]
  */
class TxReportQueue(
    val queue:    juc.BlockingQueue[ju.Map[_, _]]
) extends AnyVal {


  /** Removes all available transaction reports from
    * this queue and returns them as a list.
    *
    * This operation may be more efficient than repeatedly
    * polling this queue.
    *
    * @return a list of all available tranaction reports.
    */
  def drain(): List[TxReport] = {
    val c = new ju.LinkedList[ju.Map[_, _]]
    queue.drainTo(c)
    c.asScala.map(new TxReport(_))(breakOut)
  }


  /** Removes at most the given number of available
    * transaction reports from this queue and returns
    * them as a list.
    *
    * This operation may be more efficient than repeatedly
    * polling this queue.
    *
    * @param  maxReports  the maximum number of reports to transfer.
    * @return a list of all available tranaction reports.
    */
  def drain(maxReports: Int): List[TxReport] = {
    val c = new ju.LinkedList[ju.Map[_, _]]
    queue.drainTo(c, maxReports)
    c.asScala.map(new TxReport(_))(breakOut)
  }


  /** Retrieves and removes the head of this queue,
    * waiting up to the specified wait time
    * if necessary for an element to become available.
    *
    * Throws [[http://docs.oracle.com/javase/7/docs/api/java/lang/InterruptedException.html InterruptedException]]  if interrupted while waiting.
    *
    * @param  timeout  the duration of time to wait before giving up.
    * @return the head of this queue, or `None` if the specified
    *     waiting time elapses before an element is available.
    */
  def poll(timeout: Duration): Option[TxReport] =
    Option {
      queue.poll(timeout.toNanos, NANOSECONDS)
    } map (new TxReport(_))


  /** Retrieves and removes the head of this queue,
    * or returns `None` if this queue is empty.
    *
    * @return the head of this queue, or `None`
    *     if this queue is empty.
    */
  def poll(): Option[TxReport] = {
    Option {
      queue.poll()
    } map (new TxReport(_))
  }


  /** Retrieves and removes the head of this queue,
    * waiting if necessary until an element becomes available.
    *
    * Throws [[http://docs.oracle.com/javase/7/docs/api/java/lang/InterruptedException.html InterruptedException]]  if interrupted while waiting.
    *
    * @return the head of this queue.
    */
  def take(): TxReport =
    new TxReport(queue.take())


  /** Retrieves, but does not remove, the head of this queue,
    * or `None` if this queue is empty.
    *
    * @return the head of this queue, or `None` if this
    *     queue is empty.
    */
  def peek(): Option[TxReport] = {
    Option {
      queue.peek()
    } map (new TxReport(_))
  }


  /** Returns `true` if this queue contains no transaction reports.
    *
    * @return `true` if this queue contains no transaction reports.
    */
  def isEmpty(): Boolean =
    queue.isEmpty()


  /** Returns an iterator over the transaction reports in
    * this queue.
    *
    * @return an `Iterator` over the transaction reports
    *     in this queue.
    */
  def iterator(): Iterator[TxReport] =
    queue.iterator.asScala.map(new TxReport(_))


  /** Returns the number of transaction reports in the queue.
    *
    * @return the number of transaction reports in the queue.
    */
  def size(): Int =
    queue.size()

}
