import scala.language.reflectiveCalls

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
class DatomicSchemaQuerySpec extends Specification {
  sequential 
  val uri = "datomic:mem://datomicschemaqueryspec"

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }

  val violent = AddIdent(person.character / "violent")
  val weak    = AddIdent(person.character / "weak")
  val clever  = AddIdent(person.character / "clever")
  val dumb    = AddIdent(person.character / "dumb")

  val name      = Attribute(person / "name",      SchemaType.string, Cardinality.one) .withDoc("Person's name")
  val age       = Attribute(person / "age",       SchemaType.long,   Cardinality.one) .withDoc("Person's age")
  val character = Attribute(person / "character", SchemaType.ref,    Cardinality.many).withDoc("Person's characters")

  val schema = Seq(
    name,
    age,
    character,
    violent,
    weak,
    clever,
    dumb
  )

  def startDB = {
    println(s"created DB with uri $uri: ${createDatabase(uri)}")

    implicit val conn = Datomic.connect(uri)  
    
    Await.result(
      Datomic.transact(schema) flatMap { tx => 
        println(s"Provisioned schema... TX: $tx")

        val id = DId(Partition.USER)
        Datomic.transact(
          Entity.add(id)(
            person / "name"      -> "toto",
            person / "age"       -> 30L,
            person / "character" -> Set(weak, dumb)
          ),
          Entity.add(DId(Partition.USER))(
            person / "name"      -> "tutu",
            person / "age"       -> 54L,
            person / "character" -> Set(violent, clever)
          ),
          Entity.add(DId(Partition.USER))(
            person / "name"      -> "tata",
            person / "age"       -> 23L,
            person / "character" -> Set(weak, clever)
          )
        ) map { tx => 
          println(s"Provisioned data... TX: $tx")
        }
      },
      Duration("30 seconds")
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
          :where  [ ?e ${name} ?n ] 
                  [ ?e ${character} ?char ]
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

  }
}
