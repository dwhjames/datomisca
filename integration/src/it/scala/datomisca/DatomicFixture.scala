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

import org.scalatest.Suite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Millis, Seconds}

import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID.randomUUID


trait SampleData {
  val schema: Seq[TxData]
  val txData: Seq[TxData]
}

trait DatomicFixture extends ScalaFutures
{ self: Suite =>

  // globally set timeout to 10 seconds, with the future being checked every 100ms
  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(100, Millis))

  def withDatomicDB(testCode: Connection => Any): Unit = {
    val uri = s"datomic:mem://${randomUUID()}"
    Datomic.createDatabase(uri)
    try {
      implicit val conn = Datomic.connect(uri)
      testCode(conn)
      ()
    } finally {
      Datomic.deleteDatabase(uri)
      ()
    }
  }

  def withSampleDatomicDB(sampleData: SampleData)(testCode: Connection => Any): Unit = {
    val uri = s"datomic:mem://${randomUUID()}"
    Datomic.createDatabase(uri)
    try {
      implicit val conn = Datomic.connect(uri)
      whenReady(Datomic.transact(sampleData.schema)) { _ =>
        whenReady(Datomic.transact(sampleData.txData)) { _ =>
          testCode(conn)
        }
      }
      ()
    } finally {
      Datomic.deleteDatabase(uri)
      ()
    }
  }

}
