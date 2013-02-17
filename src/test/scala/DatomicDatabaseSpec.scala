import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._


@RunWith(classOf[JUnitRunner])
class DatomicDatabaseSpec extends Specification {
  sequential

  val uri = "datomic:mem://DatomicDatabaseSpec"

  import scala.concurrent.ExecutionContext.Implicits.global

  "Datomic Database" should {
    "filter" in {
      
      println(s"created DB with uri $uri: ${createDatabase(uri)}")
      implicit val conn = Datomic.connect(uri)

      val schema = Datomic.parseOps("""
        ;; stories
        [{:db/id #db/id[:db.part/db]
          :db/ident :story/title
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db/fulltext true
          :db/index true
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :story/url
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db/unique :db.unique/identity
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :story/slug
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}]

        ;; comments
        [{:db/id #db/id[:db.part/db]
          :db/ident :comments
          :db/valueType :db.type/ref
          :db/cardinality :db.cardinality/many
          :db/isComponent true
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :comment/body
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :comment/author
          :db/valueType :db.type/ref
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}]

        ;; users
        [{:db/id #db/id[:db.part/db]
          :db/ident :user/firstName
          :db/index true
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :user/lastName
          :db/index true
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :user/email
          :db/index true
          :db/unique :db.unique/identity
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db}
         {:db/id #db/id[:db.part/db]
          :db/ident :user/passwordHash
          :db/valueType :db.type/string
          :db/cardinality :db.cardinality/one
          :db.install/_attribute :db.part/db} 
         {:db/id #db/id[:db.part/db]
          :db/ident :user/upVotes
          :db/valueType :db.type/ref
          :db/cardinality :db.cardinality/many
          :db.install/_attribute :db.part/db}

         ;; publish time
         {:db/id #db/id[:db.part/db]
          :db/ident :publish/at
          :db/valueType :db.type/instant
          :db/cardinality :db.cardinality/one
          :db/index true
          :db.install/_attribute :db.part/db}]
      """)

      val data = Datomic.parseOps("""
        ;; stories
        [{:db/id #db/id [:db.part/user]
          :story/title "Teach Yourself Programming in Ten Years"
          :story/url "http://norvig.com/21-days.html"}
         {:db/id #db/id [:db.part/user]
          :story/title "Clojure Rationale"
          :story/url "http://clojure.org/rationale"}
         {:db/id #db/id [:db.part/user]
          :story/title "Beating the Averages"
          :story/url "http://www.paulgraham.com/avg.html"}]

        ;; users
        [{:db/id #db/id [:db.part/user]
          :user/firstName "Stu"
          :user/lastName "Halloway"
          :user/email "stuarthalloway@datomic.com"}
         {:db/id #db/id [:db.part/user]
          :user/firstName "Ed"
          :user/lastName "Itor"
          :user/email "editor@example.com"}]

        """)

      val user    = Namespace("user")
      val story   = Namespace("story")
      val publish = Namespace("publish")
      Await.result(
        for {
          _ <- Datomic.transact(schema.get)
          _ <- Datomic.transact(data.get)
          _ <- Datomic.transact(
                 Entity.add(DId(Partition.USER))(
                   user / "firstName"    -> "John",
                   user / "lastName"     -> "Doe",
                   user / "email"        -> "jdoe@example.com",
                   user / "passwordHash" -> "<SECRET>"
                 )
               )
        } yield {
          val qPasswordHash = Query.manual[Args1, Args1]("""[:find ?v :in $ :where [_ :user/passwordHash ?v]]""")

          println("Find PasswordHash:" + Datomic.q(qPasswordHash, database))

          Datomic.q(
            Query.manual[Args3, Args1]("""
              [
               :find ?e
               :in $ ?attr ?val
               :where [?e ?attr ?val]
              ]
            """),
            Datomic.database,
            DRef(user / "email"),
            DString("jdoe@example.com")
          ) map {
            case DLong(e) =>
              println(s"Found e: $e")
              database.touch(e)
          }

          val datoms = Datomic.database.datoms(DDatabase.AEVT, user / "passwordHash")
          println(s"Datoms: $datoms")

          ///////////////////////////////////////////////////////////////////
          // filtered db cannot
          val passwordHashId = Datomic.database.entid(user / "passwordHash")
          println(s"passwordHashId: $passwordHashId")

          val filteredDb = Datomic.database filter { (_, datom) =>
            datom.attrId != passwordHashId
          }

          println(s"Find PasswordHash: ${Datomic.q(qPasswordHash, filteredDb)}")

          ///////////////////////////////////////////////////////////////////
          // filter will be called for every datom, so it is idiomatic
          // to filter only on the things that need filtering. Filtered
          // and non-filtered database values can be combined, e.g.:
          /*val qCombined = Datomic.typed.query[Args3, Args1]("""
            [:find ?ent
             :in $plain $filtered ?email
             :where [$plain ?e :user/email ?email]
                    [(datomic.api/entity $filtered ?e) ?ent]
            ]
          """)

          Datomic.q(qCombined, database, filteredDb, DString("jdoe@example.com")).headOption.collect{
            case DLong(eid) =>
              database.touch(eid).foreach(e => println("Entity:"+e))
          }*/

          // add a publish/at date to a transaction
          Datomic.transact(
            Entity.add(DId(Partition.USER))(
              story / "title" -> "codeq",
              story / "url"   -> "http://blog.datomic.com/2012/10/codeq.html"
            ),
            Entity.add(DId(Partition.TX))(
              publish / "at" -> DInstant(new java.util.Date())
            )
          ) map { tx =>
            println(s"end tx: $tx")
          }

          // all the stories
          val qCount = Query.manual[Args1, Args1]("""
            [:find ?e :in $ :where [?e :story/url ]]
          """)
          val count = Datomic.q(qCount, Datomic.database).size
          println(s"Found $count entities")

          // same query, filtered to stories that have been published.
          val filteredDb2 = Datomic.database filter { (db, datom) =>
            db.entity(datom.tx).get(KW(":publish/at")).isDefined
          }

          val count2 = Datomic.q(qCount, filteredDb2).size
          println(s"Found $count2 entities")

          success
          },
        Duration("10 seconds")
      )


    }
  }
}
