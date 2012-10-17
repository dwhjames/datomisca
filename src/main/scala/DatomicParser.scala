package reactivedatomic

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.Positional

case class PositionFailure(msg: String, offsetLine: Int, offsetCol: Int)

object DatomicParser extends JavaTokenParsers {

  val literal = """[a-zA-Z]([a-zA-Z0-9.]|_[a-zA-Z0-9.])*""".r

  val stringContent = """([^"\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*""".r
  
  val noDecimal: Parser[String] = """-?\d+""".r

  val decimalMandatory: Parser[String] = """(\d+\.\d*)""".r

  def datomicString: Parser[DString] = "\"" ~> stringContent <~ "\"" ^^ { DString(_) }
  def datomicLong: Parser[DLong] = noDecimal ^^ { (s: String) => DLong(s.toLong) }
  def datomicFloat: Parser[DFloat] = decimalMandatory ^^ {  (s: String) => DFloat(s.toFloat) }
  //def datomicDouble: Parser[DDouble] = floatingPointNumber ^^ { (s: String) => DDouble(s.toDouble) }
  def datomicBoolean: Parser[DBoolean] = ("true" | "false") ^^ {  (s: String) => DBoolean(s.toBoolean) }


  // TERMS
  def datasource: Parser[DataSource] = "$" ~> opt(literal) ^^ {
    case None => ImplicitDS
    case Some(s) => ExternalDS(s)
  }
  def empty: Parser[Term] = "_" ^^ { _ => Empty }
  def keyword: Parser[Keyword] = ":" ~> opt(literal <~ "/") ~ ident ^^ { 
    case None ~ n => Keyword(n) 
    case Some(ns) ~ n => Keyword(n, ns) 
  }
  def variable: Parser[Var] = "?" ~> ident ^^ { (n: String) => Var(n) }
  def const: Parser[Const] = ( datomicBoolean | datomicString | datomicFloat | datomicLong ) ^^ { Const(_) }
  def term: Parser[Term] = empty | keyword | variable | const

  // RULES
  def ruleTerms: Parser[(Term, Term, Term)] = (term ~ term ~ term | term ~ term | term ) ^^ {
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) => (t1, t2, t3)
    case (t1: Term) ~ (t2: Term) => (t1, t2, Empty)
    case (t1: Term) => (t1, Empty, Empty)
  }
  def rule: Parser[Rule] = positioned("[" ~> opt(datasource) ~ ruleTerms <~ "]" ^^ { 
    case None ~ terms => Rule(ImplicitDS, terms._1, terms._2, terms._3)
    case Some(ds) ~ terms => Rule(ds, terms._1, terms._2, terms._3)
  })

  // WHERE
  def where: Parser[Where] = positioned(":where" ~> rep(rule) ^^ { Where(_) })

  // IN
  def inDatasource: Parser[InDataSource] = datasource ^^ { InDataSource(_) }
  def inVariable: Parser[InVariable] = variable ^^ { InVariable(_) }
  def input: Parser[reactivedatomic.Input] = inDatasource | inVariable
  def in: Parser[In] = positioned(":in" ~> rep(input) ^^ { In(_) })

  // FIND
  def outVariable: Parser[OutVariable] = variable ^^ { OutVariable(_) }
  def output: Parser[Output] = outVariable
  def find: Parser[Find] = positioned(":find" ~> rep(output) ^^ { Find(_) })

  // QUERY
  def query: Parser[Query] = "[" ~> find ~ opt(in) ~ where <~ "]" ^^ { 
    case find ~ in ~ where => Query(find, in, where) 
  }

  def parseRule(input: String): Rule = parseAll(rule, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseWhere(input: String): Where = parseAll(where, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseFind(input: String): Find = parseAll(find, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseQuery(input: String): Query = parseAll(query, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseQuerySafe(input: String): Either[PositionFailure, Query] = parseAll(query, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }
}