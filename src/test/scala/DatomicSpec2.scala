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


@RunWith(classOf[JUnitRunner])
class DatomicSpec2 extends Specification {
  "Datomic" should {
    "simple schema" in {
      import reactivedatomic._
      import reactivedatomic.Datomic._
      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val uri = "datomic:mem://datomicspec2"

      val _person = new NameSpace("person")

      val schema = Schema (
        // PERSON
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _person/'name,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A unique person name\"", 
          _db.install/'_attribute -> _db.part/'db
        )
      ) 

      println("created DB: "+createDatabase(uri))
      val conn = connect(uri)
      conn.createSchema(schema).map( r => println("Res:"+r) )

      val data = Seq(
        _db/add(
          _db/'id -> tempid( _db.part/'user ),
          _person/'name -> "toto"
        ),
        _db/add(
          _db/'id -> tempid( _db.part/'user ),
          _person/'name -> "tutu"
        )
      )
      
      Await.result(
        conn.transact(data).map( tx => println("res:"+tx) ), 
        Duration(30, SECONDS) 
      )

      query("[ :find ?e ?n :where [ ?e :person/name ?n ]]").collect {
        case Res(e: Long, n: String) => 
          val entity = database.entity(e)
          println(entity.get(":person/name"))
      }

      success
    }
  }
}