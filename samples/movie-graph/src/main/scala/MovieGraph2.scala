
import scala.language.reflectiveCalls

import datomisca._

import scala.concurrent._
import scala.concurrent.duration.Duration

object MovieGraph2Schema {

  object ns {
    val actor = Namespace("actor")
    val role  = Namespace("role")
    val movie = Namespace("movie")
  }

  val actorName = Attribute(ns.actor / "name",    SchemaType.string, Cardinality.one) .withDoc("The name of the actor")

  val actorForRole = Attribute(ns.role / "actor",     SchemaType.ref,    Cardinality.one).withDoc("The actor for this role")
  val movieForRole = Attribute(ns.role / "movie",     SchemaType.ref,    Cardinality.one).withDoc("The movie in which this role appears")
  val character    = Attribute(ns.role / "character", SchemaType.string, Cardinality.one).withDoc("The charcter name of this role")

  val movieTitle = Attribute(ns.movie / "title", SchemaType.string, Cardinality.one).withDoc("The title of the movie")
  val movieYear  = Attribute(ns. movie / "year", SchemaType.long,   Cardinality.one).withDoc("The year the movie was released")


  val txData = Seq(
    actorName,
    actorForRole, movieForRole, character,
    movieTitle, movieYear
  )
}

object MovieGraph2Data {
  import MovieGraph2Schema._

  val Carrie_Ann_Moss = SchemaFact.add(DId(Partition.USER))(actorName -> "Carrie-Ann Moss")
  
  val Hugo_Weaving    = SchemaFact.add(DId(Partition.USER))(actorName -> "Hugo Weaving")
  
  val Guy_Peace       = SchemaFact.add(DId(Partition.USER))(actorName -> "Guy Pearce")
  
  val Joe_Pantoliano  = SchemaFact.add(DId(Partition.USER))(actorName -> "Joe Pantoliano")

  val actors = Seq(Carrie_Ann_Moss, Hugo_Weaving, Guy_Peace, Joe_Pantoliano)

  val The_Matrix = (
    SchemaEntity.newBuilder
      += (movieTitle -> "The Matrix")
      += (movieYear  -> 1999)
  ) withId DId(Partition.USER)

  val The_Matrix_Reloaded = (
    SchemaEntity.newBuilder
      += (movieTitle -> "The Matrix Reloaded")
      += (movieYear  -> 2003)
  ) withId DId(Partition.USER)

  val Memento = (
    SchemaEntity.newBuilder
      += (movieTitle -> "Memento")
      += (movieYear  -> 2000)
  ) withId DId(Partition.USER)

  val movies = Seq(The_Matrix, The_Matrix_Reloaded, Memento)

  val graphNodesTxData = actors ++ movies

  val graphEdgesTxData = Seq(
    ( SchemaEntity.newBuilder
        += (actorForRole -> Carrie_Ann_Moss.id)
        += (movieForRole -> The_Matrix.id)
        += (character    -> "Trinity")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Carrie_Ann_Moss.id)
        += (movieForRole -> The_Matrix_Reloaded.id)
        += (character    -> "Trinity")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Carrie_Ann_Moss.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Natalie")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Hugo_Weaving.id)
        += (movieForRole -> The_Matrix.id)
        += (character    -> "Agent Smith")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Hugo_Weaving.id)
        += (movieForRole -> The_Matrix_Reloaded.id)
        += (character    -> "Agent Smith")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Guy_Peace.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Leonard Shelby")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Joe_Pantoliano.id)
        += (movieForRole -> The_Matrix.id)
        += (character    -> "Cypher")
    ) withId DId(Partition.USER),
    ( SchemaEntity.newBuilder
        += (actorForRole -> Joe_Pantoliano.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Teddy Gammell")
    ) withId DId(Partition.USER)
  )

  val txData = graphNodesTxData ++ graphEdgesTxData
}

object MovieGraph2Queries {

  val queryFindMovieByTitle = Query("""
    [
      :find ?title ?year
      :in $ ?title
      :where
        [?movie :movie/title ?title]
        [?movie :movie/year  ?year]
    ]
  """)

  val queryFindMovieByTitlePrefix = Query("""
    [
      :find ?title ?year
      :in $ ?prefix
      :where
        [?movie :movie/title ?title]
        [?movie :movie/year  ?year]
        [(.startsWith ^String ?title ?prefix)]
    ]
  """) // give reflection warning: should be [(.startsWith ^String ?title ?prefix)]

  val queryFindActorsInTitle = Query("""
    [
      :find ?name
      :in $ ?title
      :where
        [?movie :movie/title ?title]
        [?role  :role/movie  ?movie]
        [?role  :role/actor  ?actor]
        [?actor :actor/name  ?name]
    ]
  """)
  
  val queryFindTitlesAndRolesForActor = Query("""
    [
      :find ?character ?title
      :in $ ?name
      :where
        [?actor :actor/name     ?name]
        [?role  :role/actor     ?actor]
        [?role  :role/character ?character]
        [?role  :role/movie     ?movie]
        [?movie :movie/title    ?title]
    ]
  """)

  val queryFindMoviesThatIncludeActorsInGivenMovie = Query("""
    [
      :find ?othertitle
      :in $ ?title
      :where
        [?movie  :movie/title ?title]
        [?role1  :role/movie  ?movie1]
        [?role1  :role/actor  ?actor]
        [?role2  :role/actor  ?actor]
        [?role2  :role/movie  ?movie2]
        [?movie2 :movie/title ?othertitle]
    ]
  """)

  val queryFindAllMoviesWithRole = Query("""
    [
      :find ?title
      :in $ ?character
      :where
        [?role  :role/character ?character]
        [?role  :role/movie     ?movie]
        [?movie :movie/title    ?title]
    ]
  """)

}

object MovieGraph2 {
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

  def main(args: Array[String]) {
    /*
     * Datomic URI definition
     * This defines an in-memory database
     * named 'datomisca-imdb-graph'
     */
    val uri = "datomic:mem://datomisca-imdb-graph"

    // create the database
    Datomic.createDatabase(uri)

    /*
     * Get a connection to the database
     * and make it implicit in scope
     */
    implicit val conn = Datomic.connect(uri)

    // transact the schema, which returns a future
    val fut = Datomic.transact(MovieGraph2Schema.txData) flatMap { _ =>
      // transact the graph: the actors, movies, and roles
      Datomic.transact(MovieGraph2Data.txData)
    } map { _ =>

      def disp[T](results: Iterable[T]): Unit =
        println(s"""Results:
        |${results.mkString("[\n  ", ",\n  ", "\n]")}
        |""".stripMargin)

      disp {
        println("Find the movie 'The Matrix'")
        Datomic.q(MovieGraph2Queries.queryFindMovieByTitle, Datomic.database, "The Matrix")
      }

      disp {
        println("Find movies with titles that start with 'The Matrix'")
        Datomic.q(MovieGraph2Queries.queryFindMovieByTitlePrefix, Datomic.database, "The Matrix")
      }

      disp {
        println("Find the actors in the movie 'Memento'")
        Datomic.q(MovieGraph2Queries.queryFindActorsInTitle, Datomic.database, "Memento")
      }

      disp {
        println("Find the movie roles for actor 'Carrie-Ann Moss'")
        Datomic.q(MovieGraph2Queries.queryFindTitlesAndRolesForActor, Datomic.database, "Carrie-Ann Moss")
      }

      disp {
        println("Find the movies that included actors from 'The Matrix Reloaded'")
        Datomic.q(MovieGraph2Queries.queryFindMoviesThatIncludeActorsInGivenMovie, Datomic.database, "The Matrix Reloaded")
      }

      disp {
        println("Find all the movies with a role called 'Agent Smith'")
        Datomic.q(MovieGraph2Queries.queryFindAllMoviesWithRole, Datomic.database, "Agent Smith")
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
