/*
 * Copyright 2012 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class MovieGraphSampleSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
     with AwaitHelper
{

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

    val `Carrie-Ann Moss` = SchemaFact.add(DId(Partition.USER))(actorName -> "Carrie-Ann Moss")

    val `Hugo Weaving`    = SchemaFact.add(DId(Partition.USER))(actorName -> "Hugo Weaving")

    val `Guy Peace`       = SchemaFact.add(DId(Partition.USER))(actorName -> "Guy Pearce")

    val `Joe Pantoliano`  = SchemaFact.add(DId(Partition.USER))(actorName -> "Joe Pantoliano")

    val actors = Seq(`Carrie-Ann Moss`, `Hugo Weaving`, `Guy Peace`, `Joe Pantoliano`)


    val `The Matrix` = (
      SchemaEntity.newBuilder
        += (movieTitle -> "The Matrix")
        += (movieYear  -> 1999)
    ) withId DId(Partition.USER)

    val `The Matrix Reloaded` = (
      SchemaEntity.newBuilder
        += (movieTitle -> "The Matrix Reloaded")
        += (movieYear  -> 2003)
    ) withId DId(Partition.USER)

    val Memento = (
      SchemaEntity.newBuilder
        += (movieTitle -> "Memento")
        += (movieYear  -> 2000)
    ) withId DId(Partition.USER)

    val movies = Seq(`The Matrix`, `The Matrix Reloaded`, Memento)


    val graphNodesTxData = actors ++ movies


    def graphEdgesTxData(tempIds: Map[DId, Long]): Seq[Seq[TxData]] = Seq(
      Seq(
        SchemaFact.add(tempIds(`Carrie-Ann Moss`.id))(actorActs -> tempIds(`The Matrix`.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Trinity")
      ),
      Seq(
        SchemaFact.add(tempIds(`Carrie-Ann Moss`.id))(actorActs -> tempIds(`The Matrix Reloaded`.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Trinity")
      ),
      Seq(
        SchemaFact.add(tempIds(`Carrie-Ann Moss`.id))(actorActs -> tempIds(Memento.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Natalie")
      ),
      Seq(
        SchemaFact.add(tempIds(`Hugo Weaving`.id))(actorActs -> tempIds(`The Matrix`.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Agent Smith")
      ),
      Seq(
        SchemaFact.add(tempIds(`Hugo Weaving`.id))(actorActs -> tempIds(`The Matrix Reloaded`.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Agent Smith")
      ),
      Seq(
        SchemaFact.add(tempIds(`Guy Peace`.id))(actorActs -> tempIds(Memento.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Leonard Shelby")
      ),
      Seq(
        SchemaFact.add(tempIds(`Joe Pantoliano`.id))(actorActs -> tempIds(`The Matrix`.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Cypher")
      ),
      Seq(
        SchemaFact.add(tempIds(`Joe Pantoliano`.id))(actorActs -> tempIds(Memento.id)),
        SchemaFact.add(DId(Partition.TX))(actorRole -> "Teddy Gammell")
      )
    )
  }

  object MovieGraphQueries {
    import MovieGraphSchema._

    val queryFindMovieByTitle = Query(s"""
      [:find ?title ?year
       :in $$ ?title
       :where
         [?movie ${movieTitle} ?title]
         [?movie ${movieYear}  ?year]]
    """)

    val queryFindMovieByTitlePrefix = Query(s"""
      [:find ?title ?year
       :in $$ ?prefix
       :where
         [?movie ${movieTitle} ?title]
         [?movie ${movieYear}  ?year]
         [(.startsWith ^String ?title  ?prefix)]]
    """)

    val queryFindActorsInTitle = Query(s"""
      [:find ?name
       :in $$ ?title
       :where
         [?movie ${movieTitle} ?title]
         [?actor ${actorActs}  ?movie]
         [?actor ${actorName}  ?name]]
    """)

    val queryFindTitlesAndRolesForActor = Query(s"""
      [:find ?role ?title
       :in $$ ?name
       :where
         [?actor ${actorName}  ?name]
         [?actor ${actorActs}  ?movie ?tx]
         [?movie ${movieTitle} ?title]
         [?tx    ${actorRole}  ?role]]
    """)

    val queryFindMoviesThatIncludeActorsInGivenMovie = Query(s"""
      [:find ?othertitle
       :in $$ ?title
       :where
         [?movie ${movieTitle} ?title]
         [?actor ${actorActs}  ?movie]
         [?actor ${actorActs}  ?othermovie]
         [?othermovie ${movieTitle} ?othertitle]]
    """)

    val queryFindAllMoviesWithRole = Query(s"""
      [:find ?title
       :in $$ ?role
       :where
         [?tx    ${actorRole}  ?role]
         [?actor ${actorActs}  ?movie ?tx]
         [?movie ${movieTitle} ?title]]
    """)

  }

  "Movie Graph Sample" should "run to completion" in withDatomicDB { implicit conn =>
    import MovieGraphQueries._

    await {
      Datomic.transact(MovieGraphSchema.txData)
    }

    val txReport = await {
      Datomic.transact(MovieGraphData.graphNodesTxData)
    }

    await {
      Future.sequence {
        MovieGraphData.graphEdgesTxData(txReport.tempidMap) map Datomic.transact
      }
    }

    val db = conn.database()

    Datomic.q(queryFindMovieByTitle, db, "The Matrix") should have size (1)

    Datomic.q(queryFindMovieByTitlePrefix, db, "The Matrix") should have size (2)

    Datomic.q(queryFindActorsInTitle, db, "Memento") should have size (3)

    Datomic.q(queryFindTitlesAndRolesForActor, db, "Carrie-Ann Moss") should have size (3)

    Datomic.q(queryFindMoviesThatIncludeActorsInGivenMovie, db, "The Matrix Reloaded") should have size (3)

    Datomic.q(queryFindAllMoviesWithRole, db, "Agent Smith") should have size (2)

  }
}
