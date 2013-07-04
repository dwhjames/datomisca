import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
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
      val violent = AddIdent(person.character / "violent")
      val weak    = AddIdent(person.character / "weak", Partition.USER)
      
      val person1 = Entity.add( DId(Partition.USER) )(
        person / "name"      -> "bob",
        person / "age"       -> 30L,
        person / "character" -> Set(violent, weak)
      )

      implicit val conn = Datomic.connect(uri)

      Datomic.transact(Seq(
        violent,
        weak,
        person1
      )) map { tx => 
        println(s"Provisioned data... TX: $tx")
      }

      //println("DID:"+DId(Partition.USER).value.getClass)
      Datomic.q(Query.pure("""
        [ :find ?e ?n 
          :where  [ ?e :person/name ?n ] 
                  [ ?e :person/character :person.character/violent ]
        ]
      """), database).map {
        case Seq(DLong(e), DString(n)) => 
        println(s"PART ${datomic.Peer.part(e.underlying).getClass}")
        val entity = database.entity(e)
        println(s"Q2 entity: $e name: $n - e: ${entity.get(person / "character")}")
      }

      println("Attribute:"+Attribute( 
        person / "name",
        SchemaType.string,
        Cardinality.one
      ).withDoc("Person's name"))

      success
    }
  }
}
