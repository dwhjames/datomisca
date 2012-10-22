import reactivedatomic._
import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._
import scala.concurrent.ExecutionContext.Implicits.global

object DatomicBootstrap {
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
        _db/'ident              -> _person/'age,
        _db/'valueType          -> _db.typ/'long,
        _db/'cardinality        -> _db.cardinality/'one,
        _db/'doc                -> "\"Age of character\"", 
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
            _person/'age -> 23,
            _person/'character -> Seq( _person.character/'stupid, _person.character/'weak)
          ),
          _db/add(
            _db/'id -> tempid( _db.part/'user ),
            _person/'name -> "tutu",
            _person/'age -> 45,
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