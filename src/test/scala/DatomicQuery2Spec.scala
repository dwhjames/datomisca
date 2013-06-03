import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._

import scala.concurrent.ExecutionContext.Implicits.global


@RunWith(classOf[JUnitRunner])
class DatomicQuery2Spec extends Specification {
  sequential 
  val uri = "datomic:mem://datomicquery2spec"
  val person = Namespace("person")

  def startDB = {
    println(s"created DB with uri $uri: ${createDatabase(uri)}")

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
      ) map {
        case (DLong(e), DString(n)) => 
          val entity = database.entity(e)
          println(s"1 - entity: $e name: $n - e: ${entity.get(person / "character")}")
      }
      
      success
    }

    "2 - typed query with rule with 2 params only" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query(""" 
        [:find ?e :where [?e :person/name]]
      """)

      Datomic.q(q, database) map {
        case DLong(e) => 
          val entity = database.entity(e)
          println(s"2 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
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
      """), database, DSet(DString("toto"), DString("tata"))) map {
        case DLong(e) => 
          val entity = database.entity(e)
          println(s"3 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
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
      ) map {
        case (DLong(e), DString(n), DLong(a)) => 
          println(s"4 - entity: $e name: $n - age: $a")
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

      Datomic.q(q, database) map {
        case (DLong(e), DString(n)) => 
          println(s"5 - entity: $e name: $n")
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

      Datomic.q(q, database, totoRule) map {
        case (DLong(e), DLong(age)) => 
          println(s"e: $e - age: $age")
          age must beEqualTo(30L)
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

      Datomic.q(q, database) map {
        case (DLong(e), DString(name)) => 
          println(s"e: $e - name: $name")
          name must beEqualTo("tutu")
      }
    }
  }
}
