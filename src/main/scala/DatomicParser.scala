package reactivedatomic

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.Positional

case class PositionFailure(msg: String, offsetLine: Int, offsetCol: Int)

object DatomicParser extends JavaTokenParsers {

  val literal = """[a-zA-Z]([a-zA-Z0-9.]|_[a-zA-Z0-9.])*""".r

  val stringContent = """([^"\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*""".r
  
  val noDecimal: Parser[String] = """-?\d+""".r

  val decimalMandatory: Parser[String] = """(\d+\.\d*)""".r

  val functionLiteral = """[a-zA-Z]([a-zA-Z0-9.]|(-|_)[a-zA-Z0-9.])*""".r
  val operator: Parser[String] = "+" | "-" | "*" | "/" | "==" | "<" | ">" | "<=" | ">="

  def datomicString: Parser[DString] = "\"" ~> stringContent <~ "\"" ^^ { DString(_) }
  def datomicLong: Parser[DLong] = noDecimal ^^ { (s: String) => DLong(s.toLong) }
  def datomicFloat: Parser[DFloat] = decimalMandatory ^^ {  (s: String) => DFloat(s.toFloat) }
  //def datomicDouble: Parser[DDouble] = floatingPointNumber ^^ { (s: String) => DDouble(s.toDouble) }
  def datomicBoolean: Parser[DBoolean] = ("true" | "false") ^^ {  (s: String) => DBoolean(s.toBoolean) }

  //def dseq: Parser[DSeq] = "[" ~> rep(datomicData) <~ "]" ^^ { DSeq(_) }

  def datomicData: Parser[DatomicData] = datomicBoolean | datomicString | datomicFloat | datomicLong

  // TERMS
  def datasource: Parser[DataSource] = "$" ~> opt(literal) ^^ {
    case None => ImplicitDS
    case Some(s) => ExternalDS(s)
  }
  def empty: Parser[Term] = "_" ^^ { _ => Empty }
  def keyword: Parser[Keyword] = ":" ~> opt(literal <~ "/") ~ ident ^^ { 
    case None ~ n => Keyword(n) 
    case Some(ns) ~ n => Keyword(n, Some(Namespace(ns)))
  }

  def posKeyword: Parser[Keyword] = positioned(keyword)

  def variable: Parser[Var] = "?" ~> ident ^^ { (n: String) => Var(n) }
  def const: Parser[Const] = datomicData ^^ { Const(_) }
  def term: Parser[Term] = empty | keyword | variable | const

  // DATA RULES
  def dataRuleTerms: Parser[(Term, Term, Term)] = ( term ~ term ~ term | term ~ term | term ) ^^ {
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) => (t1, t2, t3)
    case (t1: Term) ~ (t2: Term) => (t1, t2, Empty)
    case (t1: Term) => (t1, Empty, Empty)
  }

  def dataRule: Parser[DataRule] = opt(datasource) ~ dataRuleTerms ^^ { 
    case None ~ terms => DataRule(ImplicitDS, terms._1, terms._2, terms._3)
    case Some(ds) ~ terms => DataRule(ds, terms._1, terms._2, terms._3)
  }

  // EXPRESSION RULES
  def dfunction: Parser[DFunction] = (operator | functionLiteral) ^^ { DFunction(_) }
  def dpredicate: Parser[DPredicate] = (operator | functionLiteral) ^^ { DPredicate(_) }

  def scalarBinding: Parser[ScalarBinding] = variable ^^ { ScalarBinding(_) }
  def tupleBinding: Parser[TupleBinding] = "[" ~> rep(variable) <~ "]" ^^ { TupleBinding(_) }
  def collectionBinding: Parser[CollectionBinding] = "[" ~> variable <~ "..." ~ "]" ^^ { CollectionBinding(_) }
  def relationBinding: Parser[RelationBinding] = "[[" ~> rep(variable) <~ "]]" ^^ { RelationBinding(_) }

  def binding = tupleBinding | scalarBinding | relationBinding | collectionBinding

  def functionAloneExpression: Parser[DFunction ~ Seq[Term]] = "(" ~> dfunction ~ rep(term) <~ ")"

  def functionExpression: Parser[FunctionExpression] = functionAloneExpression ~ binding ^^ {
    case ((df: DFunction) ~ (args: Seq[Term])) ~ (binding: Binding) => FunctionExpression(df, args, binding)
  }

  def predicateExpression: Parser[PredicateExpression] = "(" ~> dpredicate ~ rep(term) <~ ")" ^^ {
    case (df: DPredicate) ~ (args: Seq[_]) => PredicateExpression(df, args)
  }

  def expression: Parser[Expression] = functionExpression | predicateExpression
  def expressionRule: Parser[ExpressionRule] = expression ^^ { ExpressionRule(_) }

  // RULES
  def rule: Parser[Rule] = positioned("[" ~> (dataRule | expressionRule) <~ "]")

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
  def query: Parser[PureQuery] = "[" ~> find ~ opt(in) ~ where <~ "]" ^^ { 
    case find ~ in ~ where => PureQuery(find, in, where) 
  }

  def scalaExpr: Parser[ScalaExpr] = (("${" ~> stringContent <~ "}") | ("$" ~> literal )) ^^ { ScalaExpr(_) }
  
  def eitherScalaExprOrDatomicData = (scalaExpr | datomicData) ^^ {
    case dd: DatomicData => Right(dd)
    case se: ScalaExpr => Left(se)
  }

  def dSeqParsing: Parser[DSeqParsing] = "[" ~> rep(eitherScalaExprOrDatomicData) <~ "]" ^^ { DSeqParsing(_) }

  def parsingExpr: Parser[ParsingExpr] = scalaExpr | dSeqParsing

  def attribute: Parser[(Keyword, Either[ParsingExpr, DatomicData])] = keyword ~ (parsingExpr | datomicData) ^^ {
    case kw ~ (dd: DatomicData) => kw -> Right(dd)
    case kw ~ (se: ParsingExpr) => kw -> Left(se)
  }

  def addEntityParsing: Parser[AddEntityParsing] = "{" ~> rep(attribute) <~ "}" ^^ { t => AddEntityParsing(t.toMap) }

  def parseKeyword(input: String): Keyword = parseAll(posKeyword, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseKeywordSafe(input: String): Either[PositionFailure, Keyword] = parseAll(posKeyword, input) match {
    case Success(result, _) => Right(result)
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
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

  def parseQuery(input: String): PureQuery = parseAll(query, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => scala.sys.error(failure.msg)
  }

  def parseQuerySafe(input: String): Either[PositionFailure, PureQuery] = parseAll(query, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseAddEntityParsingSafe(input: String): Either[PositionFailure, AddEntityParsing] = parseAll(addEntityParsing, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }
}