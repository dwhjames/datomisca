import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import datomisca._
import datomisca.Datomic._

@RunWith(classOf[JUnitRunner])
class DatomicParserSpec extends Specification {
  "Datomic" should {
    "parse predicate rules" in {
      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg ?e ]
      """) must beEqualTo(DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg "toto" ]
      """) must beEqualTo(DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Const(DString("toto")) ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg 1234 ]
      """) must beEqualTo(DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Const(DLong(1234)) ))

      DatomicParser.parseRule(""" 
        [ ?n :ns1.ns2/arg 1234.234 ]
      """) must beEqualTo(DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Const(DFloat(1234.234F)) ))

      DatomicParser.parseRule(""" 
        [ $data ?n :ns1.ns2/arg true ]
      """) must beEqualTo(DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Const(DBoolean(true)) ))

      success
    }

    "parse function rules" in {
      DatomicParser.parseRule(""" 
        [ (< ?a 30) ]
      """) must beEqualTo(ExpressionRule(PredicateExpression(DPredicate("<"), Seq(Var("a"), Const(DLong(30))) )))

      DatomicParser.parseRule(""" 
        [(* ?a 12) ?months]
      """) must beEqualTo(ExpressionRule(FunctionExpression(DFunction("*"), Seq(Var("a"), Const(DLong(12))), ScalarBinding(Var("months")) )))

      DatomicParser.parseRule(""" 
        [(my-func ?a "toto") [ ?b ?c ]]
      """) must beEqualTo(ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), TupleBinding(Seq(Var("b"), Var("c"))) )))

      DatomicParser.parseRule(""" 
        [(my-func ?a "toto") [ ?b ... ]]
      """) must beEqualTo(ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), CollectionBinding(Var("b")) )))

      DatomicParser.parseRule(""" 
        [(my-func ?a "toto") [[ ?b ?c ]] ]
      """) must beEqualTo(ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), RelationBinding(Seq(Var("b"), Var("c"))) )))

    }

    "parse where" in {
      DatomicParser.parseWhere(""" 
        :where [ ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 "toto" ]
      """) must beEqualTo(
        Where(Seq(
          DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
          DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Const(DString("toto")) )
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
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Const(DString("toto")) )
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
          In(Seq(InDataSource(ImplicitDS), InVariable(ScalarBinding(Var("f"))))),
          Where(Seq(
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Var("f") )
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
          In(Seq(InDataSource(ExternalDS("data")), InVariable(ScalarBinding(Var("f"))))),
          Where(Seq(
            DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Var("f") )
          ))
        )
      )
    }

    "parse query with in & DS & func" in {
      DatomicParser.parseQuery(""" 
        [ :find ?n ?e
        :in $data ?f
        :where [ $data ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 ?f ] [(* ?a 12) ?months] ]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          In(Seq(InDataSource(ExternalDS("data")), InVariable(ScalarBinding(Var("f"))))),
          Where(Seq(
            DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Var("f") ),
            ExpressionRule(FunctionExpression(DFunction("*"), Seq(Var("a"), Const(DLong(12))), ScalarBinding(Var("months")) ))
          ))
        )
      )
    }

    "parse query with rule with 2 params only" in {
      DatomicParser.parseQuery(""" 
        [:find ?c :where [?c :community/name]]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("c")))),
          Where(Seq(
            DataRuleParsing(ImplicitDS, Var("c"), Keyword( "name", Some(Namespace("community"))), Empty )
          ))
        )
      )
      
    }

    "parse query with with, in & DS & func" in {
      DatomicParser.parseQuery(""" 
        [ :find ?n ?e
        :with ?toto
        :in $data ?f
        :where [ $data ?n :ns1.ns2/arg ?e ] [ ?n :ns/arg2 ?toto ] [(* ?a 12) ?months] ]
      """) must beEqualTo(
        Query(
          Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
          With(Seq(Var("toto"))),
          In(Seq(InDataSource(ExternalDS("data")), InVariable(ScalarBinding(Var("f"))))),
          Where(Seq(
            DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
            DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Var("toto") ),
            ExpressionRule(FunctionExpression(DFunction("*"), Seq(Var("a"), Const(DLong(12))), ScalarBinding(Var("months")) ))
          ))
        )
      )
    }

    "serialize predicate rule" in {
      
      DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") )
        .toString must beEqualTo("""[?n :ns1.ns2/arg ?e]""")
      DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Const(DString("toto")) )
        .toString must beEqualTo("""[$data ?n :ns1.ns2/arg "toto"]""")
    }

    "serialize function rule" in {
      ExpressionRule(PredicateExpression(DPredicate("<"), Seq(Var("a"), Const(DLong(30))) ))
        .toString must beEqualTo("""[ (< ?a 30) ]""")

      ExpressionRule(FunctionExpression(DFunction("*"), Seq(Var("a"), Const(DLong(12))), ScalarBinding(Var("months")) ))
        .toString must beEqualTo("""[ (* ?a 12) ?months ]""")

      ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), TupleBinding(Seq(Var("b"), Var("c"))) ))
        .toString must beEqualTo("""[ (my-func ?a "toto") [ ?b ?c ] ]""")

      ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), CollectionBinding(Var("b")) ))
        .toString must beEqualTo("""[ (my-func ?a "toto") [ ?b ... ] ]""")

      ExpressionRule(FunctionExpression(DFunction("my-func"), Seq(Var("a"), Const(DString("toto"))), RelationBinding(Seq(Var("b"), Var("c"))) ))
        .toString must beEqualTo("""[ (my-func ?a "toto") [[ ?b ?c ]] ]""")
    }

    "serialize :where" in {
      Where(Seq(
          DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
          DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Const(DLong(1234)) )
      )).toString must beEqualTo(""" 
        :where [$data ?n :ns1.ns2/arg ?e] [?n :ns/arg2 1234]
      """.trim)
    }

    "serialize :find" in {
      Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))).toString must beEqualTo(""" 
        :find ?n ?e
      """.trim)
    }

    "serialize :in" in {
      In(Seq(InDataSource(ExternalDS("data")), InVariable(ScalarBinding(Var("f"))))).toString must beEqualTo(""" 
        :in $data ?f
      """.trim)
    }

    "serialize full query" in {
      Query(
        Find(Seq(OutVariable(Var("n")), OutVariable(Var("e")))),
        With(Seq(Var("toto"))),
        In(Seq(InDataSource(ExternalDS("data")), InVariable(ScalarBinding(Var("f"))))),
        Where(Seq(
          DataRuleParsing(ExternalDS("data"), Var("n"), Keyword( "arg", Some(Namespace("ns1.ns2"))), Var("e") ),
          DataRuleParsing(ImplicitDS, Var("n"), Keyword( "arg2", Some(Namespace("ns"))), Var("f") )
        ))
      ).toString must beEqualTo(""" 
        [ :find ?n ?e :with ?toto :in $data ?f :where [$data ?n :ns1.ns2/arg ?e] [?n :ns/arg2 ?f] ]
      """.trim)
    }
  }
}
