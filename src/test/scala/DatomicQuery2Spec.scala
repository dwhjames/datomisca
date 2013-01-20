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
import Datomic._
import DatomicDataImplicits._

import scala.concurrent.ExecutionContext.Implicits.global


@RunWith(classOf[JUnitRunner])
class DatomicQuery2Spec extends Specification {
  sequential 
  val uri = "datomic:mem://datomicquery2spec"
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
    Datomic.deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic" should {
    "1 - pure query" in {
      implicit val conn = Datomic.connect(uri)
      val query = Query("""
        [ :find ?e ?n 
          :in $ ?char
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character ?char ]
        ]
      """)
      
      Datomic.q(
        query, 
        database, 
        DRef(KW(":person.character/violent"))
      ).map {
        case (e: DLong, n: DString) => 
          val entity = database.entity(e)
          println("1 - entity: "+ e + " name:"+n+ " - e:" + entity.get(person / "character"))
      }
      
      success
    }

    "2 - typed query with rule with 2 params only" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query(""" 
        [:find ?e :where [?e :person/name]]
      """)

      Datomic.q(q).map{
        case (e: DLong) => 
          val entity = database.entity(e)
          println("2 - entity: "+ e + " name:"+ entity.get(person / "name") + " - e:" + entity.get(person / "character"))
        case _ => failure("unexpected result")
      }

      success
    }

    "3 - typed query with rule with params variable length" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query(""" 
        [
         :find ?e
         :in $ [?names ...] 
         :where [?e :person/name ?names]
        ]
      """), database, DSet(DString("toto"), DString("tata"))).map{
        case (e: DLong) => 
          val entity = database.entity(e)
          println("3 - entity: "+ e + " name:"+ entity.get(person / "name") + " - e:" + entity.get(person / "character"))
        case _ => failure("unexpected result")
      }

      success
    }

    "4 - typed query with rule with list of tuple inputs" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query(""" 
        [
         :find ?e ?name ?age
         :in $ [[?name ?age]]
         :where [?e :person/name ?name]
                [?e :person/age ?age]
        ]
      """)

      Datomic.q(
        q, database, 
        DSet(
          DSet(DString("toto"), DLong(30L)),
          DSet(DString("tutu"), DLong(54L))
        )
      ).map{
        case (e: DLong, n: DString, a: DLong) => 
          println("4 - entity: "+ e + " name:"+ n + " - age:" + a)
        case _ => failure("result not expected")
      }

      success
    }

    "5 - typed query with fulltext query" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query(""" 
        [
         :find ?e ?n
         :where [(fulltext $ :person/name "toto") [[ ?e ?n ]]]
        ]
      """)

      Datomic.q(q).map{
        case (e: DLong, n: DString) => 
          println("5 - entity: "+ e + " name:"+ n)
        case _ => failure("result not expected")
      }

      success
    }

    "8 - query with rule alias" in {
      implicit val conn = Datomic.connect(uri)
      
      val totoRule = Query.rules("""
        [ [ [toto ?e]
           [?e :person/name "toto"]
        ] ]
      """)

      val q = Query("""
        [
          :find ?e ?age
          :in $ %
          :where [?e :person/age ?age]
                 (toto ?e)
        ]
      """)

      Datomic.q(q, database, totoRule).map {
        case (e: DLong, age: DLong) => 
          println(s"e: $e - age: $age")
          age must beEqualTo(DLong(30L))
        case _ => failure("unexpected result")
      }
    }

    "9 - query with with" in {
      implicit val conn = Datomic.connect(uri)
      
      val q = Query("""
        [ :find ?e ?n 
          :with ?age
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/age ?age ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """)

      Datomic.q(q).map {
        case (e: DLong, name: DString) => 
          println(s"e: $e - name: $name")
          name must beEqualTo(DString("tutu"))
        case _ => failure("unexpected result")
      }
    }
  }
}