
import scala.language.reflectiveCalls

import datomisca._

import scala.concurrent._
import scala.concurrent.duration.Duration

object MovieGraphSchema {

  object ns {
    val actor = Namespace("actor")
    val movie = Namespace("movie")
  }

  val actorName = Attribute(ns.actor / "name",    SchemaType.string, Cardinality.one) .withDoc("The name of the actor")
  val actorActs = Attribute(ns.actor / "acts-in", SchemaType.ref,    Cardinality.many).withDoc("References to the movies the actor has acted in")

  val actorRole = Attribute(ns.actor / "role",    SchemaType.string, Cardinality.one) .withDoc("The character name of a role in a movie")

  val movieTitle = Attribute(ns.movie / "title", SchemaType.string, Cardinality.one).withDoc("The title of the movie")
  val movieYear  = Attribute(ns. movie / "year", SchemaType.long,   Cardinality.one).withDoc("The year the movie was released")


  val txData = Seq(
    actorName, actorActs, actorRole,
    movieTitle, movieYear
  )
}

object MovieGraphData {
  import MovieGraphSchema._

  val Carrie_Ann_Moss = SchemaFact.add(DId(Partition.USER))(actorName -> "Carrie-Ann Moss")
  
  val Hugo_Weaving    = SchemaFact.add(DId(Partition.USER))(actorName -> "Hugo Weaving")
  
  val Guy_Peace       = SchemaFact.add(DId(Partition.USER))(actorName -> "Guy Pearce")
  
  val Joe_Pantoliano  = SchemaFact.add(DId(Partition.USER))(actorName -> "Joe Pantoliano")

  val actors = Seq(Carrie_Ann_Moss, Hugo_Weaving, Guy_Peace, Joe_Pantoliano)

  val The_Matrix = SchemaEntity.add(DId(Partition.USER))(Props() +
    (movieTitle -> "The Matrix") +
    (movieYear  -> 1999)
  )

  val The_Matrix_Reloaded = SchemaEntity.add(DId(Partition.USER))(Props() +
    (movieTitle -> "The Matrix Reloaded") +
    (movieYear  -> 2003)
  )

  val Memento = SchemaEntity.add(DId(Partition.USER))(Props() +
    (movieTitle -> "Memento") +
    (movieYear  -> 200)
  )

  val movies = Seq(The_Matrix, The_Matrix_Reloaded, Memento)

  val graphNodesTxData = actors ++ movies

  def graphEdgesTxData(tempIds: Map[DId, Long]): Seq[Seq[Operation]] = Seq(
    Seq(
      SchemaFact.add(tempIds(Carrie_Ann_Moss.id))(actorActs -> Set(DRef(tempIds(The_Matrix.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Trinity")
    ),
    Seq(
      SchemaFact.add(tempIds(Carrie_Ann_Moss.id))(actorActs -> Set(DRef(tempIds(The_Matrix_Reloaded.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Trinity")
    ),
    Seq(
      SchemaFact.add(tempIds(Carrie_Ann_Moss.id))(actorActs -> Set(DRef(tempIds(Memento.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Natalie")
    ),
    Seq(
      SchemaFact.add(tempIds(Hugo_Weaving.id))(actorActs -> Set(DRef(tempIds(The_Matrix.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Agent Smith")
    ),
    Seq(
      SchemaFact.add(tempIds(Hugo_Weaving.id))(actorActs -> Set(DRef(tempIds(The_Matrix_Reloaded.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Agent Smith")
    ),
    Seq(
      SchemaFact.add(tempIds(Guy_Peace.id))(actorActs -> Set(DRef(tempIds(Memento.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Leonard Shelby")
    ),
    Seq(
      SchemaFact.add(tempIds(Joe_Pantoliano.id))(actorActs -> Set(DRef(tempIds(The_Matrix.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Cypher")
    ),
    Seq(
      SchemaFact.add(tempIds(Joe_Pantoliano.id))(actorActs -> Set(DRef(tempIds(Memento.id)))),
      SchemaFact.add(DId(Partition.TX))(actorRole -> "Teddy Gammell")
    )
  )
}

object MovieGraphQueries {

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
        [(.startsWith ?title ?prefix)]
    ]
  """) // give reflection warning: should be [(.startsWith ^String ?title ?prefix)]

  val queryFindActorsInTitle = Query("""
    [
      :find ?name
      :in $ ?title
      :where
        [?movie :movie/title   ?title]
        [?actor :actor/acts-in ?movie]
        [?actor :actor/name    ?name]
    ]
  """)
  
  val queryFindTitlesAndRolesForActor = Query("""
    [
      :find ?role ?title
      :in $ ?name
      :where
        [?actor :actor/name    ?name]
        [?actor :actor/acts-in ?movie ?tx]
        [?movie :movie/title   ?title]
        [?tx    :actor/role    ?role]
    ]
  """)

  val queryFindMoviesThatIncludeActorsInGivenMovie = Query("""
    [
      :find ?othertitle
      :in $ ?title
      :where
        [?movie :movie/title   ?title]
        [?actor :actor/acts-in ?movie]
        [?actor :actor/acts-in ?othermovie]
        [?othermovie :movie/title ?othertitle]
    ]
  """)

  val queryFindAllMoviesWithRole = Query("""
    [
      :find ?title
      :in $ ?role
      :where
        [?tx    :actor/role    ?role]
        [?actor :actor/acts-in ?movie ?tx]
        [?movie :movie/title   ?title]
    ]
  """)

}

object MovieGraph {
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
    val fut = Datomic.transact(MovieGraphSchema.txData) flatMap { _ =>
      // transact the graph nodes: the actors and the movies
      Datomic.transact(MovieGraphData.graphNodesTxData)
    } flatMap { tx =>
      // transact the graph edges in sequence: the acts-in relationships
      Future.sequence { MovieGraphData.graphEdgesTxData(tx.tempidMap) map Datomic.transact }
    } map { _ =>

      def disp[T](results: Iterable[T]): Unit =
        println(s"""Results:
        |${results.mkString("[\n  ", ",\n  ", "\n]")}
        |""".stripMargin)

      disp {
        println("Find the movie 'The Matrix'")
        Datomic.q(MovieGraphQueries.queryFindMovieByTitle, Datomic.database, DString("The Matrix"))
      }

      disp {
        println("Find movies with titles that start with 'The Matrix'")
        Datomic.q(MovieGraphQueries.queryFindMovieByTitlePrefix, Datomic.database, DString("The Matrix"))
      }

      disp {
        println("Find the actors in the movie 'Memento'")
        Datomic.q(MovieGraphQueries.queryFindActorsInTitle, Datomic.database, DString("Memento"))
      }

      disp {
        println("Find the movie roles for actor 'Carrie-Ann Moss'")
        Datomic.q(MovieGraphQueries.queryFindTitlesAndRolesForActor, Datomic.database, DString("Carrie-Ann Moss"))
      }

      disp {
        println("Find the movies that included actors from 'The Matrix Reloaded'")
        Datomic.q(MovieGraphQueries.queryFindMoviesThatIncludeActorsInGivenMovie, Datomic.database, DString("The Matrix Reloaded"))
      }

      disp {
        println("Find all the movies with a role called 'Agent Smith'")
        Datomic.q(MovieGraphQueries.queryFindAllMoviesWithRole, Datomic.database, DString("Agent Smith"))
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
