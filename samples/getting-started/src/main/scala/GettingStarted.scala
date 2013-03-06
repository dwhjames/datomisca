
import datomisca._
import Datomic._
import DatomicMapping._

import scala.concurrent._
import scala.concurrent.duration.Duration

object PersonSchema {
  // Namespaces definition to be reused in Schema
  object ns {
    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }
  }
  // Attributes
  val name       = Attribute(ns.person / "name",       SchemaType.string,  Cardinality.one) .withDoc("The name of a person")
  val age        = Attribute(ns.person / "age",        SchemaType.long,    Cardinality.one) .withDoc("The age of a person")
  val birth      = Attribute(ns.person / "birth",      SchemaType.instant, Cardinality.one) .withDoc("The birth date of a person")
  val characters = Attribute(ns.person / "characters", SchemaType.ref,     Cardinality.many).withDoc("The characteristics of a person")

  // Characters enumerated values
  val violent = AddIdent(ns.person.character / "violent")
  val weak    = AddIdent(ns.person.character / "weak")
  val clever  = AddIdent(ns.person.character / "clever")
  val dumb    = AddIdent(ns.person.character / "dumb")
  val stupid  = AddIdent(ns.person.character / "stupid")

  // Schema
  val txData = Seq(
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
  val uri = "datomic:mem://datomisca-getting-started"

  // Datomic Connection as an implicit in scope
  implicit lazy val conn = Datomic.connect(uri)

  def main(args: Array[String]) {
    Datomic.createDatabase(uri)

    // Loads Schema
    val res = Datomic.transact(PersonSchema.txData) flatMap { _ =>
      // John temporary ID
      val johnId = DId(Partition.USER)
      // John person entity
      val john = SchemaEntity.add(johnId)(Props() +
        (PersonSchema.name       -> "John") +
        (PersonSchema.age        -> 35L) +
        (PersonSchema.birth      -> new java.util.Date()) +
        // Please note that we use Datomic References here
        (PersonSchema.characters -> Set( PersonSchema.violent.ref, PersonSchema.clever.ref ))
      )

      // creates an entity
      Datomic.transact(john).map{ tx =>

        println(s"Real JohnId: ${tx.resolve(johnId)}")

        val queryFindByName = Query("""
          [ :find ?e ?age
            :in $ ?name
            :where [?e :person/name ?name]
                   [?e :person/age ?age]
          ]
        """)

        val results = Datomic.q(queryFindByName, database, DString("John"))
        println(results)
        results.headOption map {
          case (DLong(eid), _) =>
            // retrieves again the entity directly by its ID
            val entity = database.entity(eid)

            val johnName       = entity(PersonSchema.name)
            val johnAge        = entity(PersonSchema.age)
            val johnBirth      = entity(PersonSchema.birth)
            val johnCharacters = entity.read[Set[DRef]](PersonSchema.characters)

            println(s"John's -\n\tname:\t$johnName\n\tage:\t$johnAge\n\tbirth:\t$johnBirth\n\tcharacteristics:\t$johnCharacters")            
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
