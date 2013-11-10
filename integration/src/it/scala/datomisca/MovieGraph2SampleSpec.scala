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


class MovieGraph2SampleSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
     with AwaitHelper
{

  object MovieGraph2Schema {

    object ns {
      val actor = Namespace("actor")
      val role  = Namespace("role")
      val movie = Namespace("movie")
    }

    val actorName = Attribute(ns.actor / "name", SchemaType.string, Cardinality.one) .withDoc("The name of the actor")

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


    val graphEdgesTxData = Seq(
      (SchemaEntity.newBuilder
        += (actorForRole -> `Carrie-Ann Moss`.id)
        += (movieForRole -> `The Matrix`.id)
        += (character    -> "Trinity")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Carrie-Ann Moss`.id)
        += (movieForRole -> `The Matrix Reloaded`.id)
        += (character    -> "Trinity")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Carrie-Ann Moss`.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Natalie")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Hugo Weaving`.id)
        += (movieForRole -> `The Matrix`.id)
        += (character    -> "Agent Smith")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Hugo Weaving`.id)
        += (movieForRole -> `The Matrix Reloaded`.id)
        += (character    -> "Agent Smith")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Guy Peace`.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Leonard Shelby")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Joe Pantoliano`.id)
        += (movieForRole -> `The Matrix`.id)
        += (character    -> "Cypher")
      ) withId DId(Partition.USER),
      (SchemaEntity.newBuilder
        += (actorForRole -> `Joe Pantoliano`.id)
        += (movieForRole -> Memento.id)
        += (character    -> "Teddy Gammell")
      ) withId DId(Partition.USER)
    )

    val txData = graphNodesTxData ++ graphEdgesTxData
  }

  object MovieGraph2Queries {
    import MovieGraph2Schema._

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
         [?movie ${movieTitle}   ?title]
         [?role  ${movieForRole} ?movie]
         [?role  ${actorForRole} ?actor]
         [?actor ${actorName}    ?name]]
    """)

    val queryFindTitlesAndRolesForActor = Query(s"""
      [:find ?role ?title
       :in $$ ?name
       :where
         [?actor ${actorName}    ?name]
         [?role  ${actorForRole} ?actor]
         [?role  ${character}    ?character]
         [?role  ${movieForRole} ?movie]
         [?movie ${movieTitle}   ?title]]
    """)

    val queryFindMoviesThatIncludeActorsInGivenMovie = Query(s"""
      [:find ?othertitle
       :in $$ ?title
       :where
         [?movie  ${movieTitle}   ?title]
         [?role1  ${movieForRole} ?movie1]
         [?role1  ${actorForRole} ?actor]
         [?role2  ${actorForRole} ?actor]
         [?role2  ${movieForRole} ?movie2]
         [?movie2 ${movieTitle}   ?othertitle]]
    """)

    val queryFindAllMoviesWithRole = Query(s"""
      [:find ?title
       :in $$ ?character
       :where
         [?role  ${character}    ?character]
         [?role  ${movieForRole} ?movie]
         [?movie ${movieTitle}   ?title]]
    """)

  }

  "Movie Graph 2 Sample" should "run to completion" in withDatomicDB { implicit conn =>
    import MovieGraph2Queries._

    await {
      Datomic.transact(MovieGraph2Schema.txData)
    }

    await {
      Datomic.transact(MovieGraph2Data.txData)
    }

    val db = conn.database

    Datomic.q(queryFindMovieByTitle, db, DString("The Matrix")) should have size (1)

    Datomic.q(queryFindMovieByTitlePrefix, db, DString("The Matrix")) should have size (2)

    Datomic.q(queryFindActorsInTitle, db, DString("Memento")) should have size (3)

    Datomic.q(queryFindTitlesAndRolesForActor, db, DString("Carrie-Ann Moss")) should have size (3)

    Datomic.q(queryFindMoviesThatIncludeActorsInGivenMovie, db, DString("The Matrix Reloaded")) should have size (3)

    Datomic.q(queryFindAllMoviesWithRole, db, DString("Agent Smith")) should have size (2)

  }
}
