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
class DatomicQuerySpec extends Specification {
  "Datomic" should {
    "query simple" in {
      import Datomic._
      import DatomicData._

      implicit val uri = "datomic:mem://datomicqueryspec"
      DatomicBootstrap(uri)

      query("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """).collect {
        case Res(e: Long, n: String) => 
          val entity = database.entity(DLong(e))
          println("Q1 entity: "+ e + " name:"+n+ " - e:" + entity.get(":person/character"))
      }

      q2("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """).collect {
        case List(e: DLong, n: DString) => 
          val entity = database.entity(e)
          println("Q2 entity: "+ e + " name:"+n+ " - e:" + entity.get(":person/character"))
      }

      q2("""
        [ :find ?e ?name
          :where  [ ?e :person/name ?name ] 
                  [ ?e :person/age ?age ] 
                  [ (< ?age 30) ]
        ]
      """).collect {
        case List(e: DLong, name: DString) => 
          name must beEqualTo(DString("toto"))
      }

      /*q3[(DLong, DString, DInt)]( 
      """
        [ :find ?e ?name ?age
          :where  [ ?e :person/name ?name ] 
                  [ ?e :person/age ?age ] 
                  [ ?e :person/character ?char ]
        ]
      """
      ).map( list => 
        list.map {
          case (e: DLong, n: DString, a: DInt) => 
            val entity = database.entity(e.value)
            println("Q3 entity: e:%s name:%s age:%s chars:%s".format(e, n, a, entity.get(":person/character")))
        }
      ).recover{ 
        case e => println(e.getMessage) 
      }*/

      success
    }
  }
}