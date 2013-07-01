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
class DatomicQuerySpec extends Specification {
  sequential
  val uri = "datomic:mem://datomicqueryspec"
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

  "Datomic" should {
    "1 - pure query" in {

      implicit val conn = Datomic.connect(uri)


      Datomic.q(Query.pure("""
        [ :find ?e ?n
          :where  [ ?e :person/name ?n ]
                  [ ?e :person/character :person.character/violent ]
        ]
      """), database) map {
        case List(DLong(e), DString(n)) =>
          val entity = database.entity(e)
          println(s"1 - entity: $e name: $n - e: ${entity.get(person / "character")}")
      }

      success
    }

    "2 - typed query with rule with 2 params only" in {

      implicit val conn = Datomic.connect(uri)

      val q = Query.manual[Args0, Args1]("""
        [:find ?e :where [?e :person/name]]
      """)
      Datomic.q(q, database) map {
        case DLong(e) =>
          val entity = database.entity(e)
          println(s"2 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
      }

      success
    }

    "3 - typed query with rule with params variable length" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query.manual[Args2, Args1]("""
        [
         :find ?e
         :in $ [?names ...]
         :where [?e :person/name ?names]
        ]
      """), database, Datomic.coll("toto", "tata")) map {
        case DLong(e) =>
          val entity = database.entity(e)
          println(s"3 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
      }

      success
    }

    "4 - typed query with rule with list of tuple inputs" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query.manual[Args2, Args3]("""
        [
         :find ?e ?name ?age
         :in $ [[?name ?age]]
         :where [?e :person/name ?name]
                [?e :person/age ?age]
        ]
      """)
      Datomic.q(
        q, database,
        DColl(
          Datomic.coll("toto", 30L),
          Datomic.coll("tutu", 54L)
        )
      ) map {
        case (DLong(e), DString(n), DLong(a)) =>
          println(s"4 - entity: $e name: $n - age: $a")
      }

      success
    }

    "5 - typed query with fulltext query" in {

      implicit val conn = Datomic.connect(uri)
      val q = Query.manual[Args0, Args2]("""
        [
         :find ?e ?n
         :where [(fulltext $ :person/name "toto") [[ ?e ?n ]]]
        ]
      """)
      Datomic.q(q, database) map {
        case (DLong(e), DString(n)) =>
          println(s"5 - entity: $e name: $n")
      }

      success
    }

    "6 - serialize rule alias" in {

      val alias = DRuleAliases(
        Seq(DRuleAlias(
          "region",
          Seq(Var("c"), Var("r")),
          Seq(
            DataRule(ImplicitDS, Var("c"), Keyword( "neighborhood", Some(Namespace("community"))), Var("n") ),
            DataRule(ImplicitDS, Var("n"), Keyword( "district", Some(Namespace("neighborhood"))), Var("d") ),
            DataRule(ImplicitDS, Var("d"), Keyword( "region", Some(Namespace("district"))), Var("re") ),
            DataRule(ImplicitDS, Var("re"), Keyword( "ident", Some(Namespace("db"))), Var("r") )
          )
        ))
      )

      alias.toNative.trim must beEqualTo(
        ( "[ [ [region ?c ?r]" +
          " [?c :community/neighborhood ?n]" +
          " [?n :neighborhood/district ?d]" +
          " [?d :district/region ?re]" +
          " [?re :db/ident ?r] ] ]").trim
      )
    }

    "7 - parse rule alias" in {
      val alias = DRuleAliases(
        Seq(DRuleAlias(
          "region",
          Seq(Var("c"), Var("r")),
          Seq(
            DataRule(ImplicitDS, Var("c"), Keyword( "neighborhood", Some(Namespace("community"))), Var("n") ),
            DataRule(ImplicitDS, Var("n"), Keyword( "district", Some(Namespace("neighborhood"))), Var("d") ),
            DataRule(ImplicitDS, Var("d"), Keyword( "region", Some(Namespace("district"))), Var("re") ),
            DataRule(ImplicitDS, Var("re"), Keyword( "ident", Some(Namespace("db"))), Var("r") )
          )
        ))
      )

      Query.rules("""
        [ [ [region ?c ?r]
           [?c :community/neighborhood ?n]
           [?n :neighborhood/district ?d]
           [?d :district/region ?re]
           [?re :db/ident ?r]
        ] ]
      """) must beEqualTo(alias)
    }

    "8 - query with rule alias" in {
      implicit val conn = Datomic.connect(uri)

      val totoRule = Query.rules("""
        [ [ [toto ?e]
           [?e :person/name "toto"]
        ] ]
      """)

      val q = Query.manual[Args2, Args2]("""
        [
          :find ?e ?age
          :in $ %
          :where [?e :person/age ?age]
                 (toto ?e)
        ]
      """)

      Datomic.q(q, database, totoRule) map {
        case (DLong(e), DLong(age)) =>
          println(s"e: $e - age: $age")
          age must beEqualTo(30L)
      }
    }

    "9 - query with with" in {
      implicit val conn = Datomic.connect(uri)

      val q = Query.manual[Args0, Args2]("""
        [ :find ?e ?n
          :with ?age
          :where  [ ?e :person/name ?n ]
                  [ ?e :person/age ?age ]
                  [ ?e :person/character :person.character/violent ]
        ]
      """)

      Datomic.q(q, database) map {
        case (DLong(e), DString(name)) =>
          println(s"e: $e - name: $name")
          name must beEqualTo("tutu")
      }
    }

    "10 - parse fct call in rule alias" in {
      val alias = DRuleAliases(
        Seq(DRuleAlias(
          "region",
          Seq(Var("c"), Var("r")),
          Seq(
            DataRule(ImplicitDS, Var("c"), Keyword( "neighborhood", Some(Namespace("community"))), Var("n") ),
            DataRule(ImplicitDS, Var("n"), Keyword( "district", Some(Namespace("neighborhood"))), Var("d") ),
            DataRule(ImplicitDS, Var("d"), Keyword( "region", Some(Namespace("district"))), Var("re") ),
            RuleAliasCall("rule", Seq(Var("b"), Const(DLong(12)))),
            DataRule(ImplicitDS, Var("re"), Keyword( "ident", Some(Namespace("db"))), Var("r") )
          )
        ))
      )

      Query.rules("""
        [ [ [region ?c ?r]
           [?c :community/neighborhood ?n]
           [?n :neighborhood/district ?d]
           [?d :district/region ?re]
           (rule ?b 12)
           [?re :db/ident ?r]
        ] ]
      """) must beEqualTo(alias)
    }

    "11 - parse fct call in rule alias" in {
      val alias = DRuleAliases(
        Seq(DRuleAlias(
          "region",
          Seq(Var("c"), Var("r")),
          Seq(
            DataRule(ImplicitDS, Var("c"), Keyword( "neighborhood", Some(Namespace("community"))), Var("n") ),
            DataRule(ImplicitDS, Var("n"), Keyword( "district", Some(Namespace("neighborhood"))), Var("d") ),
            DataRule(ImplicitDS, Var("d"), Keyword( "region", Some(Namespace("district"))), Var("re") ),
            RuleAliasCall("rule", Seq(Var("b"), Const(DLong(12)))),
            DataRule(ImplicitDS, Var("re"), Keyword( "ident", Some(Namespace("db"))), Var("r") )
          )
        ))
      )

      Query.rules("""
        [ [ (region ?c ?r)
           [?c :community/neighborhood ?n]
           [?n :neighborhood/district ?d]
           [?d :district/region ?re]
           (rule ?b 12)
           [?re :db/ident ?r]
        ] ]
      """) must beEqualTo(alias)
    }
  }

  "12 - passing database when no :in" in {

    implicit val conn = Datomic.connect(uri)

    val q = Query("""
      [:find ?e :where [?e :person/name]]
    """)
    Datomic.q(q, database) map {
      case DLong(e) =>
        val entity = database.entity(e)
        println(s"12 - entity: $e name: ${entity.get(person / "name")} - e: ${entity.get(person / "character")}")
    }

    success
  }

  "13 - passing datasource when no :in" in {

    implicit val conn = Datomic.connect(uri)

    val q = Query("""
      [:find ?firstname ?lastname :where [?firstname ?lastname]]
    """)
    Datomic.q(q, DColl(Datomic.coll("John", "Smith"))) map {
      case (DString(firstname), DString(lastname)) =>
        firstname must beEqualTo("John")
        lastname must beEqualTo("Smith")
        println(s"13 - firstname: $firstname lastname: $lastname")
    }

    success
  }

  "14 - passing datasource with :in" in {

    implicit val conn = Datomic.connect(uri)

    val q = Query("""
      [:find ?firstname ?lastname :in $ :where [?firstname ?lastname]]
    """)
    Datomic.q(q, DColl(Datomic.coll("John", "Smith"))) map {
      case (DString(firstname), DString(lastname)) =>
        firstname must beEqualTo("John")
        lastname must beEqualTo("Smith")
        println(s"14 - firstname: $firstname lastname: $lastname")
    }

    success
  }
}
