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

package object `:community` {
  val name = "name"
}


@RunWith(classOf[JUnitRunner])
class DatomicSpec extends Specification {
  "Datomic" should {
    "query simple" in {
      /*println("Creating and connecting to database...")

      val uri = "datomic:mem://seattle"
      Peer.createDatabase(uri)
      val conn = Peer.connect(uri)

      val results = Peer.q("[:find ?c :where [?c :community/name]]", conn.db())*/

      //println("1:" + Thread.currentThread.getContextClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs.exists(_.getFile.contains("datomic")))
      //println("2:" + Thread.currentThread.getContextClassLoader.getParent.asInstanceOf[{val parentA: java.net.URLClassLoader}].parentA.getURLs.exists(_.getFile.contains("datomic")))
      import reactivedatomic._
      import reactivedatomic.Datomic._
      import scala.concurrent.ExecutionContext.Implicits.global

      implicit val uri = "datomic:mem://seattle"

      val _community = new NameSpace("community") {
        val orgtype = NameSpace("community.orgtype")
        val typ = NameSpace("community.type")
      }

      val _neighborhood = new NameSpace("neighborhood") 

      val _district = new NameSpace("district") 
      val _region = new NameSpace("region") 

      val schema = Schema (
        // COMMUNITY
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'name,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A unique district name (upsertable)\"", 
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'url,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A community's url\"", 
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'neighborhood,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A community's neighborhood\"", 
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'category,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'many,
          _db/'doc                -> "\"All community categories\"", 
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'orgtype,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A community orgtype enum value\"", 
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add( 
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _community/'type,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A community type enum value\"", 
          _db.install/'_attribute -> _db.part/'db
        ),

        // community/orgtype
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'community ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'commercial ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'nonprofit ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'personal ),

        // community/type enum values
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/"email-list" ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'twitter ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/"facebook-page" ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'blog ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'website ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'wiki ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'myspace ),
        _db/add( tempid( _db.part/'user ), _db/'ident, _community.typ/'ning ),

        // neighborhood
        _db/add(
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _neighborhood/'name,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'unique             -> _db.unique/'identity,
          _db/'doc                -> "\"A unique neighborhood name (upsertable)\"",
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add(
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _neighborhood/'district,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A neighborhood's district\"",
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add(
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _neighborhood/'district,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A neighborhood's district\"",
          _db.install/'_attribute -> _db.part/'db
        ),

        // district
        _db/add(
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _district/'name,
          _db/'valueType          -> _db.typ/'string,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'unique             -> _db.unique/'identity,
          _db/'doc                -> "\"A unique district name (upsertable)\"",
          _db.install/'_attribute -> _db.part/'db
        ),
        _db/add(
          _db/'id                 -> tempid( _db.part/'db ),
          _db/'ident              -> _district/'region,
          _db/'valueType          -> _db.typ/'ref,
          _db/'cardinality        -> _db.cardinality/'one,
          _db/'doc                -> "\"A district region enum value\"",
          _db.install/'_attribute -> _db.part/'db
        ),

        // district/region enum values
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'n),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'ne),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'e),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'se),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'s),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'sw),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'w),
        _db/add( tempid( _db.part/'user ), _db/'ident, _region/'nw)
      ) 


      /*++
      {
        implicit val id = tempid(_db.part/'db)
        
        Seq( 
          _db/add( _db/'ident, _community/'name ),
          _db/add( _db/'valueType, _db.typ/'string ),
          _db/add( _db/'cardinality, _db.cardinality/'one ),
          _db/add( _db/'doc, "\"A unique district name (upsertable)\""), 
          _db/add( _db.part/'db, _db.install/'attribute, id)
        )
      } :+
      _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'community ) :+
      _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'commercial ) :+
      _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'nonprofit ) :+
      _db/add( tempid( _db.part/'user ), _db/'ident, _community.orgtype/'personal )*/


      println("created DB: "+createDatabase(uri))
      val conn = connect(uri)

      println("Schema:"+schema.asJava)

      //DummyDatomic.fakeSchema(uri

      conn.createSchema(schema).map( r => println("Res:"+r) )

      val data_rdr = new FileReader("samples/seattle/seattle-data0.dtm")
      val data_tx: java.util.List[Object] = datomic.Util.readAll(data_rdr).get(0).asInstanceOf[java.util.List[Object]]
      data_rdr.close()
      println("DATA:"+data_tx)
      val txResult2 = conn.connection.transact(data_tx).get()
      println(txResult2)

        /*SchemaAttribute(
          ":db/id" -> "#db/id[:db.part/db]",
          ":db/ident" -> ":community/name",
          ":db/valueType" -> ":db.type/string",
          ":db/cardinality" -> ":db.cardinality/one",
          ":db/fulltext" -> true,
          ":db/doc" -> "\"A community's name\"",
          ":db.install/_attribute" -> ":db.part/db"
        ) :+ */

        /*SchemaAttribute { 
          implicit val id = tempid(_db.part/'db)
          
          Seq( 
            _db/add( _db/'ident, _community/'name ),
            _db/add( _db/'valueType, _db.typ/'string ),
            _db/add( _db/'cardinality, _db.cardinality/'one ),
            _db/add( _db/'doc, "\"A unique district name (upsertable)\""), 
            _db/add( _db.install/'_attribute, _db.part/'db)
          )
        }*/
  



      /*
        {:db/id #db/id[:db.part/db]
        :db/ident :community/name
        :db/valueType :db.type/string
        :db/cardinality :db.cardinality/one
        :db/fulltext true
        :db/doc "A community's name"
        :db.install/_attribute :db.part/db}
      */
      //DummyDatomic.bootstrap(uri)
      /*Attribute(
        ":db/id" -> "#db/id[:db.part/db]",
        ":db/ident" -> ":community/name",
        ":db/valueType" -> ":db.type/string",
        ":db/cardinality" -> ":db.cardinality/one"
        ":db/fulltext" -> true,
        ":db/doc" -> "A community's name",
        ":db.install/_attribute" -> ":db.part/db"
      )*/

      //Operation(":db/add", "#db/id[:db.part/user]", ":db/ident", ":region/n")
      //Datomic.Schema(
      //  withId(tempid("db")) {
      //_db/add( _db/'ident, `:community`.name )
      //  }
      //)


      /*println(
        _db/add( tempid(_db.part/'user), _db/'ident, _community/'name )
      )*/

      /*SchemaAttribute(
        ":db/id" -> "#db/id[:db.part/db]",
        ":db/ident" -> ":community/name",
        ":db/valueType" -> ":db.type/string",
        ":db/cardinality" -> ":db.cardinality/one",
        ":db/fulltext" -> true,
        ":db/doc" -> "A community's name",
        ":db.install/_attribute" -> ":db.part/db"
      )

      SchemaAttribute { 
        implicit val id = tempid(_db.part/'user)
        
        Seq( 
          _db/add( _db/'ident, _community/'name ),
          _db/add( _db/'valueType, _db.typ/'string ),
          _db/add( _db/'cardinality, _db.cardinality/'one ),
          _db/add( _db/'doc, "A unique district name (upsertable)"), 
          _db/add( _db.install/'_attribute, _db.part/'db)
        )
      }

      SchemaAttribute( 
        _db/'id -> tempid( _db.part/'user ),
        _db/'ident -> _community/'name,
        _db/'valueType -> _db.typ/'string,
        _db/'cardinality -> _db.cardinality/'one,
        _db/'doc -> "A unique district name (upsertable)", 
        _db.install/'_attribute -> _db.part/'db
      )

      SchemaAttribute(
        id = tempid("user"),
        ident = ":community/name",
        valueType = ":db.type/string",
        cardinality = ":db.cardinality/one",
        doc = "A unique district name (upsertable)"
      ).install()

      SchemaAttribute(
        id = tempid(_db.part/'user),
        ident = _community/'name,
        valueType = _db.typ/'string,
        cardinality = _db.cardinality/'one,
        doc = "A unique district name (upsertable)"
      ).install()

      Add(tempid("user"), ":db/ident", ":region/n")

      Add(tempid(_db.part/'user), _db/'ident, ":region/n")
      Add(tempid(_db.part/'user), _db/'ident, ":region/ne")
      Add(tempid(_db.part/'user), _db/'ident, ":region/e")

      val _region = NameSpace(":region")

      Add(tempid(_db.part/'user), _db/'ident, _region/'se)
      Add(tempid(_db.part/'user), _db/'ident, _region/'s)
      Add(tempid(_db.part/'user), _db/'ident, _region/'sw)
      Add(tempid(_db.part/'user), _db/'ident, _region/'w)

      case class Community(name: String, url: String)
      object Communities extends Schema[Community] {
        def name = SchemaAttribute(tempid(_db.part/'user), _community/'name, _db.typ/'string, _db.cardinality/'one, "A unique district name (upsertable)")
        def url = SchemaAttribute(tempid(_db.part/'user), _community/'url, _db.typ/'string, _db.cardinality/'one, "A unique district url (upsertable)")

        def * = name ~ url
      }

      for(c <- Communities if(c.name == "blabla")) yield(c)*/

      // [:find ?e :in $data ?age :where [$data ?e :age ?age]]
      /*
      results = Peer.q("[:find ?e ?aname ?v ?added" +
           ":in $ [[?e ?a ?v _ ?added]] " +
           ":where " +
           "[?e ?a ?v _ ?added]" +
           "[?a :db/ident ?aname]]",
           report.get(Connection.DB_AFTER),
           report.get(Connection.TX_DATA));
      */
      /*var e = ??("e")
      var a = ??("age")

      FIND( ??("e"), ??("a") ) IN( $data ) WHERE { (e, a, $data) =>
        Datom($data, e, ::("age"), a) 
      }
      WHERE Datom( ??("e"), ::("age"), ??("a") ).collect
      in(db)

      find(??("e"), ??("name"), ??("v"), ??("added")) in( $data, )*/

      /*val results = Datomic.query("[:find ?c ?n :where [?c :community/name ?n]]")

      results.collect{
        case Res(id: Long, name: String) => 
          println("Found id:%s name:%s".format(id, name))
          val entity = database.entity(id)
          println(entity.keySet())
      }*/
      
      /*println(results.toList)
      val id = results.toList(0).toList(0).asInstanceOf[Long]
      val entity = database.entity(id)
      println(entity.keySet())*/

      /*def rules(e: ??, name: ??, age: Int) = {
        List(Datom(e, Const("name"), name)) :+ Datom(e, Const("age"), Const(age))
      }

      val q: (Database, Int) => List[(Long, String)] = 
        Find(Var[Long]("e"), Var[String]("name"))
        .in( Input[DataSource]("$data"), Input[Int]("age"))
        .where{ case (e, name, $data, age) => 
          List(
            Datom(e, Const( _community/name ), name),
            Datom(e, Const( _community/age ), age)
          )
        }

      Datom(??("e"), ":community/name", ("name")) :+ Datom($$("e"), ":community/name", ??("name"))

      q(database, 45).collect { case Res(id: Long, name: String) =>
        println("Found id:%s name:%s".format(id, name))
        val entity = database.entity(id)
        println(entity.keySet())
      }*/

        //.in($$, Input(45))
        /*.where{ case (e, name, $data, age) => rules(e, name, age) }
        .query()
        .collect{ case(e: Long, name: String) =>
          //blabla
          e
        }*/

      /*println(
        FIND( ??("e"), ??("a") ).IN( $$, ??("rep")).WHERE{
          case e :: a :: $$ :: Datom(f, g, h, _, i) => 
            Rule(e, ::("age"), a)
            :+ Rule(e, ::("name"), f)
        }.collect{
          case (e: Long, a: Int)
        }
      )*/

      success
    }
  }
}