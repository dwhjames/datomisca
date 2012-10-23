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
import java.util.concurrent.TimeUnit._

import reactivedatomic._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicTransacSpec extends Specification {
  "Datomic" should {
    "operation simple" in {
      import Datomic._
      import DatomicData._

      implicit val uri = "datomic:mem://datomicqueryspec"
      DatomicBootstrap(uri)

      val violent: Operation = Add(":db/ident" -> ":person.character/violent")
      val weak: Operation = Add(":db/ident" -> ":person.character/weak")

      Add("""
        { 
          :db/id #db/id[:db.part/user -1000002]
          :person/name "tutu"
          :person/age $age
          :person/character [ ":person.character/violent" ":person.character/weak" ]
        }
      """)

      Add(
        ":person/name" -> "tutu",
        ":person/age" -> 45,
        ":person/character" -> Seq( violent.id, weak.id )
      )


      success
    }
  }
}