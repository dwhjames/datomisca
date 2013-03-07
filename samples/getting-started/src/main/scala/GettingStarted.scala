
import datomisca._
import Datomic._
import DatomicMapping._

import scala.concurrent._
import scala.concurrent.duration.Duration

import java.util.{Date => JDate}

object PersonSchema {
  // Namespaces definition to be reused in Schema
  object ns {
    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }
  }
  // Attributes
  val name      = Attribute(ns.person / "name",   SchemaType.string,  Cardinality.one) .withDoc("The name of a person")
  val age       = Attribute(ns.person / "age",    SchemaType.long,    Cardinality.one) .withDoc("The age of a person")
  val birth     = Attribute(ns.person / "birth",  SchemaType.instant, Cardinality.one) .withDoc("The birth date of a person")
  val interests = Attribute(ns.person / "traits", SchemaType.ref,     Cardinality.many).withDoc("The interests of a person")

  // Characters enumerated values
  val cooking = AddIdent(ns.person.character / "cooking")
  val sports  = AddIdent(ns.person.character / "sports")
  val travel  = AddIdent(ns.person.character / "travel")
  val movies  = AddIdent(ns.person.character / "movies")
  val books   = AddIdent(ns.person.character / "books")

  // Schema
  val txData = Seq(
    name, age, birth, interests,
    cooking, sports, travel, movies, books
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

      // An temporary identity for a new entity for 'Jane'
      val janeId = DId(Partition.USER)
      // A person entity for 'Jane'
      val jane = Entity.add(janeId)(
        PersonSchema.name.ident        -> "Jane",
        PersonSchema.ns.person / "age" -> 30,
        KW(":person/birth")            -> new JDate,
        // Please note that we use Datomic References here
        PersonSchema.interests.ident -> Set( PersonSchema.movies.ref, PersonSchema.books.ref )
      )

      // An temporary identity for a new entity for 'John'
      val johnId = DId(Partition.USER)
      // A person entity for 'John'
      val john = SchemaEntity.add(johnId)(Props() +
        (PersonSchema.name       -> "John") +
        (PersonSchema.age        -> 31) +
        (PersonSchema.birth      -> new JDate) +
        // Please note that we use Datomic References here
        (PersonSchema.interests -> Set( PersonSchema.sports.ref, PersonSchema.travel.ref ))
      )

      // creates an entity
      Datomic.transact(jane, john) map { tx =>

        println(s"Temporary identity for Jane: $janeId")
        println(s"Temporary identity for John: $johnId")
        println()
        println(s"Persisted identity for Jane: ${tx.resolve(janeId)}")
        println(s"Persisted identity for John: ${tx.resolve(johnId)}")

        val queryFindByName = Query("""
          [
            :find ?e ?name ?age ?birth
            :in $ ?limit
            :where
              [?e :person/name  ?name]
              [?e :person/age   ?age]
              [?e :person/birth ?birth]
              [(< ?age ?limit)]
          ]
        """)

        val results = Datomic.q(queryFindByName, database, DLong(32)) sortBy (_._3.as[Long])
        println(results)
        results map {
          case (DLong(eid), DString(qname), DLong(qage), DInstant(qbirth)) =>
            // retrieves again the entity directly by its ID
            val entity = database.entity(eid)

            val name      = entity(PersonSchema.name)
            assert(qname == name)

            val Some(age) = entity.get(PersonSchema.age)
            assert(qage == age)

            val birth     = entity.as[JDate](KW(":person/birth"))
            assert(qbirth == birth)

            val interests = entity.read[Set[DRef]](PersonSchema.interests)
            assert(interests.size == 2)

            println(s"""$name's
            |  age:       $age
            |  birth:     $birth
            |  interests: $interests""".stripMargin)
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
