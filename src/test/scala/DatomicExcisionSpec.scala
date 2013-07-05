
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
class DatomicExcisionSpec extends Specification {
  sequential
  val uri = "datomic:mem://DatomicExcisionSpec"
  val person = Namespace("person")

  def startDB = {
    println(s"created DB with uri $uri: ${createDatabase(uri)}")

    implicit val conn = Datomic.connect(uri)

    Await.result(
      DatomicBootstrap(uri),
      Duration("3 seconds")
    )
  }

  def stopDB = {
    Datomic.deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "DatomicExcision" should {
    "full entity excision" in {
      implicit val conn = Datomic.connect(uri)

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent)
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "full entity excision with excisionId" in {
      implicit val conn = Datomic.connect(uri)

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e, DId(Partition.USER))
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent)
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "partial entity excision" in {
      implicit val conn = Datomic.connect(uri)

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e, person / "name")
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent)
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "full entity excision before date" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e).before(new java.util.Date())
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "full entity excision before tx" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e).before(database.basisT)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "partial entity excision before tx" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      println("BEGIN:"+database.basisT)

      val maybeRes = Datomic.transact(
        Excise.entity(e, person / "name").before(database.basisT)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println("AFTER:"+tx)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "attribute excision before tx" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      val maybeRes = Datomic.transact(
        Excise.attribute( person / "age", basisT)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "attribute excision before date" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      val maybeRes = Datomic.transact(
        Excise.attribute( person / "age", new java.util.Date)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "attribute excision ALL" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      val maybeRes = Datomic.transact(
        Excise.attribute( person / "age")
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }

    "attribute excision before tx with excisionId" in {
      implicit val conn = Datomic.connect(uri)

      val basisT = database.basisT

      val query = Query("""
        [ :find ?e
          :in $ ?name
          :where  [ ?e :person/name ?name ]
        ]
      """)

      val DLong(e) = Datomic.q(query, database, DString("toto")).head

      val maybeRes = Datomic.transact(
        Excise.attribute( person / "age", DId(Partition.USER), basisT)
      ).map{ tx =>
        val toto = tx.dbAfter.entity(e)
        println(s"toto ${toto.id} excised:"+toto.toMap)

        Datomic.q(Query("""
          [:find ?e :in $ ?excised :where [?e :db/excise ?excised]]
        """), database, DLong(e)).head match {
          case DLong(ent) => println("found excision entity:"+ent); success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )
    }
  }
}
