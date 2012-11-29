import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

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

import reactivedatomic._
import scala.concurrent.ExecutionContext.Implicits.global


import Datomic._
import DatomicData._


@RunWith(classOf[JUnitRunner])
class DatomicQuerySpec extends Specification {
  sequential 
  val uri = "datomic:mem://datomicqueryspec"
  val person = Namespace("person")

  def startDB = {
    println("Creating DB with uri %s: %s".format(uri, createDatabase(uri)))

    implicit val conn = Datomic.connect(uri)  
    
    Await.result(
      DatomicBootstrap(uri),
      Duration("3 seconds")
    )
  } 

  def stopDB = {
    deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic" should {
    "1 - pure query" in {

      implicit val conn = Datomic.connect(uri)


      pureQuery("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """).all().execute().collect {
        case List(e: DLong, n: DString) => 
          val entity = database.entity(e)
          println("1 - entity: "+ e + " name:"+n+ " - e:" + entity.get(person / "character"))
      }
      
      success
    }

    "2 - typed query with rule with 2 params only" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.query[Args0, Args1](""" 
        [:find ?e :where [?e :person/name]]
      """).all().execute().map { _.map{
        case (e: DLong) => 
          val entity = database.entity(e)
          println("2 - entity: "+ e + " name:"+ entity.get(person / "name") + " - e:" + entity.get(person / "character"))
      }}

      success
    }

    "3 - typed query with rule with params variable length" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.query[Args2, Args1](""" 
        [
         :find ?e
         :in $ [?names ...] 
         :where [?e :person/name ?names]
        ]
      """).all().execute(database, DSet(DString("toto"), DString("tata"))).map{ _.map{
        case (e: DLong) => 
          val entity = database.entity(e)
          println("3 - entity: "+ e + " name:"+ entity.get(person / "name") + " - e:" + entity.get(person / "character"))
      }}

      success
    }
  }
}