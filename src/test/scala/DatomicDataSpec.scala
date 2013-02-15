import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomic.Entity
import datomic.Connection
import datomic.Database
import datomic.Peer
import datomic.Util

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import java.io.Reader
import java.io.FileReader

import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import datomisca._
import Datomic._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicDataSpec extends Specification {
  "DatomicData" should {
    "extract KW from DRef" in {
      val ns = Namespace("foo")
      DRef(ns / "bar") match {
        case DRef.IsKeyword(kw) if kw == KW(":foo/bar") => success
        case _ => failure
      }
    }
    "extract ID from DRef" in {
      DRef(DId(Partition.USER)) match {
        case DRef.IsId(id) => success
        case _ => failure
      }
    }
  }
}