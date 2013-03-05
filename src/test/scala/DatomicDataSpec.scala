import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import Datomic._

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