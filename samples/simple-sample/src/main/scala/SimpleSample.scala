
import datomisca._
import Datomic._

import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

object Person {
  // Namespaces definition to be reused in Schema
  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }

  // Attributes
  val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's name")
  val birth = Attribute( person / "birth", SchemaType.instant, Cardinality.one).withDoc("Person's birth date")
  val characters =  Attribute( person / "characters", SchemaType.ref, Cardinality.many).withDoc("Person's characterS")

  // Characters enumerated values
  val violent = AddIdent(person.character / "violent")
  val weak = AddIdent(person.character / "weak")
  val clever = AddIdent(person.character / "clever")
  val dumb = AddIdent(person.character / "dumb")
  val stupid = AddIdent(person.character / "stupid")

  // Schema
  val schema = Seq(
    name, age, birth, characters,
    violent, weak, clever, dumb, stupid
  )

}

object GettingStarted {
  // IF RUNNING FROM SBT RUNTIME :
  // This imports a helper Execution Context provided by Datomisca
  // to enhance default Scala one with access to ExecutorService
  // to be able to shut the service down after program execution.
  // Without this shutdown, when running in SBT, at second execution,
  // you get weird Clojure cache execution linked to classloaders issues...
  //
  // IF NOT IN SBT RUNTIME :
  // You can use classic Scala global execution context
  import datomisca.executioncontext.ExecutionContextHelper._

  // Datomic URI definition
  val uri = "datomic:mem://datomisca-simple-sample"

  // Datomic Connection as an implicit in scope
  implicit lazy val conn = Datomic.connect(uri)

  def main(args: Array[String]) {
    Datomic.createDatabase(uri)

    // Loads Schema
    val res = Datomic.transact(Person.schema).flatMap{ _ =>
      // John temporary ID
      val johnId = DId(Partition.USER)
      // John person entity
      val john = Entity.add(johnId)(
        Person.person / "name"       -> "John",
        Person.person / "age"        -> 35L,
        Person.person / "birth"      -> new java.util.Date(),
        // Please note that we use Datomic References here
        Person.person / "characters" -> Set( Person.violent.ref, Person.clever.ref )
      )

      // creates an entity
      Datomic.transact(john).map{ tx =>
        val realJohnId = tx.resolve(johnId)

        println(s"Real JohnId: $realJohnId")

        val queryFindByName = Query("""
          [ :find ?e ?age
            :in $ ?name
            :where [?e :person/name ?name]
                   [?e :person/age ?age]
          ]
        """)

        val results = Datomic.q(queryFindByName, database, DString("John"))
        println(results)
        results.headOption.map{
          case (e: DLong, age: DLong) =>
            // retrieves again the entity directly by its ID
            val entity = database.entity(e)

            val johnName = entity.as[String](Person.person / "name")
            val johnAge = entity.as[Long](Person.person / "age")
            val johnBirth = entity.as[java.util.Date](Person.person / "birth")
            val johnCharacters = entity.as[Set[DRef]](Person.person / "characters")

            println(s"john: $johnName $johnAge $johnBirth $johnCharacters")
        }
      }

    }

    Await.result(res, Duration("2 seconds"))

    // IF RUNNING FROM SBT RUNTIME :
    // without this, in SBT, if you run the program 2x, it fails
    // with weird cache exception linked to the way SBT manages
    // execution context and classloaders...
    defaultExecutorService.shutdownNow()
  }
}