import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

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
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicTransacSpec extends Specification {
  "Datomic" should {
    "operation simple" in {

      val uri = "datomic:mem://DatomicTransacSpec"

      Await.result(
        DatomicBootstrap(uri),
        Duration("3 seconds")
      )

      val person = new Namespace("person") {
        val character = Namespace("person.character")
      }

      //val violent = Enum(Keyword("violent", person.character)) //":person.character/violent"
      //val weak = Enum(Keyword("weak", person.character))

      //val toto = Add( Fact(Id(Partition.USER), Keyword("ident", Namespace.DB), DString(":person/toto")) )
      //val toto = Add( Id(Partition.USER), Keyword("ident", Namespace.DB), DString(":person/toto") )
      val violent = AddIdent(Keyword(person.character, "violent"))
      val weak = AddIdent(Keyword(person.character, "weak"), Partition.USER)
      
      val person1 = AddEntity( DId(Partition.USER) )(
        Keyword(Namespace("person"), "name") -> DString("bob"),
        Keyword(Namespace("person"), "age") -> DLong(30L),
        Keyword(Namespace("person"), "character") -> DSet( violent.ref, weak.ref )
      )

      implicit val conn = Datomic.connect(uri)

      Datomic.transact(Seq(
        violent,
        weak,
        person1
      )).map{ tx => 
        println("Provisioned data... TX:%s".format(tx))
      }.recover{
        case e => println(e.getMessage)
      }

      //println("DID:"+DId(Partition.USER).value.getClass)
      Datomic.q(Query.pure("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """)).map {
        case List(DLong(e), DString(n)) => 
        println("PART"+datomic.Peer.part(e.underlying).getClass)
        val entity = database.entity(e)
        println("Q2 entity: "+ e + " name:"+n+ " - e:" + entity.get(person / "character"))
      }

      println("Attribute:"+Attribute( 
        Keyword(Namespace("person"), "name"),
        SchemaType.string,
        Cardinality.one
      ).withDoc("Person's name"))

      success
    }
  }
}