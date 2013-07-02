
import scala.language.reflectiveCalls

import datomisca._
import Datomic._
import DatomicMapping._

import scala.concurrent._
import scala.concurrent.duration.Duration

import java.util.{Date => JDate}

/**
  * An object to hold the schema data for the Person entity
  */
object PersonSchema {
  /**
    * The namespaces used in the schema.
    * We put them in an object ns for
    * asthetic purposes.
    */
  object ns {
    val person = new Namespace("person") {
      /*
       * Note that we need,
       * import scala.language.reflectiveCalls
       * as the type of person is Namespace { val character: Namespace }
       */
      val character = Namespace("person.character")
    }
  }
  /*
   * The attributes of the person entity
   */
  val name      = Attribute(ns.person / "name",      SchemaType.string,  Cardinality.one) .withDoc("The name of a person")
  val age       = Attribute(ns.person / "age",       SchemaType.long,    Cardinality.one) .withDoc("The age of a person")
  val birth     = Attribute(ns.person / "birth",     SchemaType.instant, Cardinality.one) .withDoc("The birth date of a person")
  val interests = Attribute(ns.person / "interests", SchemaType.ref,     Cardinality.many).withDoc("The interests of a person")

  /*
   * A set of keyword identified entities.
   * A standard Datomic pattern to implement
   * an enumeration.
   */
  val cooking = AddIdent(ns.person.character / "cooking")
  val sports  = AddIdent(ns.person.character / "sports")
  val travel  = AddIdent(ns.person.character / "travel")
  val movies  = AddIdent(ns.person.character / "movies")
  val books   = AddIdent(ns.person.character / "books")

  /*
   * The transaction data for the schema.
   * This is simply a sequence of the
   * attributes and idents
   */
  val txData = Seq(
    name, age, birth, interests,
    cooking, sports, travel, movies, books
  )

}

object GettingStarted {
  /*
   * IF RUNNING FROM SBT RUNTIME :
   * This imports a helper Execution Context provided by Datomisca
   * to enhance default Scala one with access to ExecutorService
   * to be able to shut the service down after program execution.
   * Without this shutdown, when running in SBT, at second execution,
   * you get weird Clojure cache execution linked to classloaders issues...
   *
   * IF NOT IN SBT RUNTIME :
   * You can use classic Scala global execution context
   */
  import datomisca.executioncontext.ExecutionContextHelper._

  /*
   * Datomic URI definition
   * This defines an in-memory database
   * named 'datomisca-getting-started'
   */
  val uri = "datomic:mem://datomisca-getting-started"

  // create the database
  Datomic.createDatabase(uri)

  /*
   * Get a connection to the database
   * and make it implicit in scope
   */
  implicit val conn = Datomic.connect(uri)

  def main(args: Array[String]) {

    // transact the schema, which returns a future
    val fut = Datomic.transact(PersonSchema.txData) flatMap { _ =>

      /*
       * Create a temporary entity identity
       * We will use this to create a person
       * entity for 'Jane'
       */
      val janeId = DId(Partition.USER)

      /*
       * A person entity for 'Jane'
       * It is defined using pairs of
       * keywords and values that can be
       * cast to Datomic data types
       */
      val jane = Entity.add(janeId)(
        // the keyword ident for PersonSchema.name attribute
        PersonSchema.name.ident        -> "Jane",
        // the keyword constructed from namespaces
        PersonSchema.ns.person / "age" -> 30,
        // a raw keyword
        KW(":person/birth")            -> new JDate,
        // The set of references to the 'interests' idents
        PersonSchema.interests.ident -> Set( PersonSchema.movies, PersonSchema.books )
      )

      // Another temporary identity for a new entity for 'John'
      val johnId = DId(Partition.USER)

      /*
       * A person entity for 'John'
       * It is defined using pairs of
       * attributes and values that can
       * be converted to the Datomic type
       * of the attribute
       */
      val john = SchemaEntity.add(johnId)(Props() +
        (PersonSchema.name       -> "John") +
        (PersonSchema.age        -> 31) +
        (PersonSchema.birth      -> new JDate) +
        // Please note that we use Datomic References here
        (PersonSchema.interests -> Set( PersonSchema.sports, PersonSchema.travel ))
      )

      // tranasact the transaction data for the jane and john entities
      Datomic.transact(jane, john) map { tx =>

        println(s"Temporary identity for Jane: $janeId")
        println(s"Temporary identity for John: $johnId")
        println()
        println(s"Persisted identity for Jane: ${tx.resolve(janeId)}")
        println(s"Persisted identity for John: ${tx.resolve(johnId)}")
        println()

        /*
         * Construct a query, the syntax of which
         * is validated at compile time.
         *
         * This query finds the entity id of
         * persons (and their name, age, and birth)
         * who are younger than the given age
         * limit.
         */
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

        // execute the query and sort the results by age
        val results = Datomic.q(queryFindByName, database, DLong(32)) sortBy (_._3.as[Long])

        println(s"""Results:
        |${results.mkString("[\n  ", ",\n  ", "\n]")}
        |""".stripMargin)

        // map over the results of the query
        results map {
          // extract the values from each query result tuple
          case (DLong(eid), DString(qname), DLong(qage), DInstant(qbirth)) =>

            // load the entity by its entity id
            val entity = database.entity(eid)

            /*
             * get the value for the name attribute
             * the type of name is String, which is
             * uniquely determined by the type of
             * the attribute
             */
            val name      = entity(PersonSchema.name)
            assert(qname == name)

            /*
             * Similarly for entity.apply(..),
             * .get returns an Option[T], where T
             * is infered
             */
            val Some(age) = entity.get(PersonSchema.age)
            assert(qage == age)

            /*
             * Another approach is to explicitly give a
             * keyword and a type, which retrieves the
             * value and casts it.
             */
            val birth     = entity.as[JDate](KW(":person/birth"))
            assert(qbirth == birth)

            /*
             * For reference attributes, the value may be a
             * regular entity, or it may be an ident.
             * entity(PersonSchema.interests): Set[DatomicData]
             * So the .read[T] method allows us to cast to
             * a type of our choice. The type must be consistent
             * with the type defined by the attribute.
             */
            val interests = entity.read[Set[Keyword]](PersonSchema.interests)
            assert(interests.size == 2)

            println(s"""$name's
            |  age:       $age
            |  birth:     $birth
            |  interests: $interests
            |""".stripMargin)
        }
      }
    }

    // await the result of the future
    Await.result(fut, Duration("2 seconds"))

    Datomic.shutdown(true)

    // IF RUNNING FROM SBT RUNTIME : 
    // without this, in SBT, if you run the program 2x, it fails
    // with weird cache exception linked to the way SBT manages
    // execution context and classloaders...
    defaultExecutorService.shutdownNow()
  }
}
