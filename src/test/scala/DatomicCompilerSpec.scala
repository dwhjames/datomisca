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
class DatomicCompilerSpec extends Specification {
  "Datomic" should {
    "query simple" in {
      val uri = "datomic:mem://DatomicCompilerSpec"
      Await.result(
      DatomicBootstrap(uri).map { tx =>
        
        implicit val conn = Datomic.connect(uri)

        val person = Namespace("person")
  
        val query = Query.manual[Args2, Args2]("""
          [ :find ?e ?name
            :in $ ?age
            :where  [ ?e :person/name ?name ] 
                    [ ?e :person/age ?age ]
                    [ ?e :person/character :person.character/violent ]
          ]
        """)

        val qf = Datomic.q(query, Datomic.database, DLong(54L)).collect {
          case (DLong(e), DString(n)) => 
            val entity = database.entity(e)
            println("Q2 entity: "+ e + " name:"+n+ " - e:" + entity.get(person / "character"))
            n must beEqualTo("tutu")
        }
        
        Datomic.q(
          Query.manual[Args2, Args3]("""
            [ :find ?e ?name ?age
              :in $ ?age
              :where  [ ?e :person/name ?name ] 
                      [ ?e :person/age ?a ]
                      [ (< ?a ?age) ]
            ]
          """
        ), Datomic.database, DLong(30)).map{
          case (DLong(entity), DString(name), DLong(age)) => 
            println(s"""Q3 entity: $entity - name: $name - age: $age""")
            name must beEqualTo("tata")

          case _ => failure("unexpected types")
        }

        success
      },
      Duration("3 seconds")
    )

    Datomic.deleteDatabase(uri)
    }
  }
}