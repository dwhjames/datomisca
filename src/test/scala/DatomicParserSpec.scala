import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomic.Entity
import datomic.Connection
import datomic.Database
import datomic.Peer
import datomic.Util

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import java.io.Reader
import java.io.FileReader

import scala.concurrent._
import scala.concurrent.util._
import java.util.concurrent.TimeUnit._

import reactivedatomic._
import reactivedatomic.Datomic._
import scala.concurrent.ExecutionContext.Implicits.global

import DatomicSerializers._

@RunWith(classOf[JUnitRunner])
class DatomicParserSpec extends Specification {
  "Datomic" should {
    "parse rules" in {
      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg ?e ]
      """) must beEqualTo(Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg "toto" ]
      """) must beEqualTo(Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Const(DString("toto")) ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg 1234 ]
      """) must beEqualTo(Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Const(DLong(1234)) ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg 1234.234 ]
      """) must beEqualTo(Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Const(DFloat(1234.234F)) ))

      DatomicParser.parseRule(""" 
        [ $data ?n :ns1.ns2/arg true ]
      """) must beEqualTo(Rule(ExternalDS("data"), Var("n"), Keyword( "arg", "ns1.ns2"), Const(DBoolean(true)) ))

      success
    }

    "parse where" in {
      DatomicParser.parseWhere(""" 
        :where [ ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 "toto" ]
      """) must beEqualTo(
        Where(Seq(
          Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
          Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Const(DString("toto")) )
        ))
      )
    }

    "parse find" in {
      DatomicParser.parseFind(""" 
        :find ?n ?e
      """) must beEqualTo(
        Find(Seq(OutVariable(Var("n")), OutVariable(Var("e"))))
      )
    }

    "parse query" in {
      DatomicParser.parseQuery(""" 
        [ :find ?n ?e
        :where [ ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 "toto" ] ]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          Where(Seq(
            Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
            Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Const(DString("toto")) )
          ))
        )
      )
    }

    "parse query with in" in {
      DatomicParser.parseQuery(""" 
        [ :find ?n ?e
        :in $ ?f
        :where [ ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 ?f ] ]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          In(Seq(InDataSource(ImplicitDS), InVariable(Var("f")))),
          Where(Seq(
            Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
            Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Var("f") )
          ))
        )
      )
    }

    "parse query with in & DS" in {
      DatomicParser.parseQuery(""" 
        [ :find ?n ?e
        :in $data ?f
        :where [ $data ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 ?f ] ]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          In(Seq(InDataSource(ExternalDS("data")), InVariable(Var("f")))),
          Where(Seq(
            Rule(ExternalDS("data"), Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
            Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Var("f") )
          ))
        )
      )
    }

    "serialize rule" in {
      ruleSerialize(
        Rule(ImplicitDS, Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") )
      ) must beEqualTo("""[ ?n :ns1.ns2/arg ?e ]""")
      ruleSerialize(
        Rule(ExternalDS("data"), Var("n"), Keyword( "arg", "ns1.ns2"), Const(DString("toto")) )
      ) must beEqualTo("""[ $data ?n :ns1.ns2/arg "toto" ]""")
    }

    "serialize :where" in {
      whereSerialize(
        Where(Seq(
          Rule(ExternalDS("data"), Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
          Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Const(DInt(1234)) )
        ))
      ) must beEqualTo(""" 
        :where [ $data ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 1234 ]
      """.trim)
    }

    "serialize :find" in {
      findSerialize(
        Find(Seq(OutVariable(Var("n")), OutVariable(Var("e"))))
      ) must beEqualTo(""" 
        :find ?n ?e
      """.trim)
    }

    "serialize :in" in {
      inSerialize(
        In(Seq(InDataSource(ExternalDS("data")), InVariable(Var("f"))))
      ) must beEqualTo(""" 
        :in $data ?f
      """.trim)
    }

    "serialize full query" in {
      querySerialize(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          In(Seq(InDataSource(ExternalDS("data")), InVariable(Var("f")))),
          Where(Seq(
            Rule(ExternalDS("data"), Var("n"), Keyword( "arg", "ns1.ns2"), Var("e") ),
            Rule(ImplicitDS, Var("n"), Keyword( "arg2", "ns"), Var("f") )
          ))
        )
      ) must beEqualTo(""" 
        [ :find ?n ?e :in $data ?f :where [ $data ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 ?f ] ]
      """.trim)
    }
  }
}