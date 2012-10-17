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
class DatomicCompilerSpec extends Specification {
  "Datomic" should {
    "query simple" in {
      import Datomic._
      import DatomicData._

      println("TOTO")

      println(DatomicSerializers.querySerialize(
      DatomicCompiler.query("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ]
                  [ ?e :person/character :person.character/violent ]
        ]
      """))
      )

      success
    }
  }
}