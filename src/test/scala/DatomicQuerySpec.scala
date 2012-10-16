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

object Boostrap {
  def apply(theUri: String) = {
    import reactivedatomic.DatomicExp._
    import reactivedatomic.NameSpace

    implicit val uri = theUri //"datomic:mem://datomicspec2"

    val _person = new NameSpace("person") {
      val character = new NameSpace("person.character")
    }

    val schema = Schema (
      // PERSON
      _db/add( 
        _db/'id                 -> tempid( _db.part/'db ),
        _db/'ident              -> _person/'name,
        _db/'valueType          -> _db.typ/'string,
        _db/'cardinality        -> _db.cardinality/'one,
        _db/'doc                -> "\"A unique person name\"", 
        _db.install/'_attribute -> _db.part/'db
      ),
      _db/add( 
        _db/'id                 -> tempid( _db.part/'db ),
        _db/'ident              -> _person/'character,
        _db/'valueType          -> _db.typ/'ref,
        _db/'cardinality        -> _db.cardinality/'many,
        _db/'doc                -> "\"Traits of character\"", 
        _db.install/'_attribute -> _db.part/'db
      ),
      _db/add( tempid( _db.part/'user ), _db/'ident, _person.character/'stupid ),
      _db/add( tempid( _db.part/'user ), _db/'ident, _person.character/'clever ),
      _db/add( tempid( _db.part/'user ), _db/'ident, _person.character/'dumb ),
      _db/add( tempid( _db.part/'user ), _db/'ident, _person.character/'violent ),
      _db/add( tempid( _db.part/'user ), _db/'ident, _person.character/'weak )
    ) 

    println("created DB with uri %s: %s".format(uri, createDatabase(uri)))
    val conn = connect(uri)
    Await.result(
      conn.createSchema(schema).flatMap{ r => 
        println("created Schema...") 

        val data = Seq(
          _db/add(
            _db/'id -> tempid( _db.part/'user ),
            _person/'name -> "toto",
            _person/'character -> Seq( _person.character/'stupid, _person.character/'weak)
          ),
          _db/add(
            _db/'id -> tempid( _db.part/'user ),
            _person/'name -> "tutu",
            _person/'character -> Seq( _person.character/'clever, _person.character/'violent)
          )
        )

        conn.transact(data).map{ tx => 
          println("Provisioned data...")
        }
      }, 
      Duration(30, SECONDS) 
    )
  }
}

@RunWith(classOf[JUnitRunner])
class DatomicQuerySpec extends Specification {
  "Datomic" should {
    "query simple" in {
      import reactivedatomic.Datomic._

      implicit val uri = "datomic:mem://datomicqueryspec"
      Boostrap(uri)

      query("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """).collect {
        case Res(e: Long, n: String) => 
          val entity = database.entity(e)
          println("name:"+n+ " - e:" + entity.get(":person/character"))
      }

      success
    }
  }
}