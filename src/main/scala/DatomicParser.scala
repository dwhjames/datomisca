package reactivedatomic

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{Positional, Reader}
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

case class PositionFailure(msg: String, offsetLine: Int, offsetCol: Int)

/** technical structures used by parsing */
sealed trait ParsingExpr
case class ScalaExpr(expr: String) extends ParsingExpr
case class DSetParsing(elts: Seq[Either[ParsingExpr, DatomicData]]) extends ParsingExpr

case class DIdParsing(partition: Partition, id: Option[Long] = None)

sealed trait OpParsing
case class AddToEntityParsing(props: Map[Keyword, Either[ParsingExpr, DatomicData]]) extends OpParsing
case class FactParsing(id: Either[ParsingExpr, DIdParsing], attr: Keyword, value: Either[ParsingExpr, DatomicData])
case class AddParsing(fact: FactParsing) extends OpParsing
case class RetractParsing(fact: FactParsing) extends OpParsing
case class RetractEntityParsing(entid: Either[ParsingExpr, DLong]) extends OpParsing

object DatomicParser extends JavaTokenParsers {
  import scala.annotation.tailrec

  // skips clojure comments
  protected override val whiteSpace = """(;;.*|,*\s)+""".r

  val literal = """[a-zA-Z]([a-zA-Z0-9.]|_[a-zA-Z0-9.])*""".r
  val stringContent = """([^"\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*""".r
  
  val keywordIdent = """[a-zA-Z_]([a-zA-Z0-9.]|(-|_)[a-zA-Z0-9.])*""".r

  val noDecimal: Parser[String] = """-?\d+""".r

  val negNoDecimal: Parser[String] = """-\d+""".r

  val decimalMandatory: Parser[String] = """(\d+\.\d*)""".r

  val functionLiteral = """[a-zA-Z.]([a-zA-Z0-9.]|(-|_)[a-zA-Z0-9.])*""".r
  val operator: Parser[String] = "+" | "-" | "*" | "/" | "==" | "<" | ">" | "<=" | ">="

  def datomicString: Parser[DString] = "\"" ~> stringContent <~ "\"" ^^ { DString(_) }
  def datomicLong: Parser[DLong] = noDecimal ^^ { (s: String) => DLong(s.toLong) }
  def datomicFloat: Parser[DFloat] = decimalMandatory ^^ {  (s: String) => DFloat(s.toFloat) }
  //def datomicDouble: Parser[DDouble] = floatingPointNumber ^^ { (s: String) => DDouble(s.toDouble) }
  def datomicBoolean: Parser[DBoolean] = ("true" | "false") ^^ {  (s: String) => DBoolean(s.toBoolean) }

  def did: Parser[DId] = "#db/id" ~> "[" ~> (partition ~ opt(negNoDecimal)) <~ "]" ^^ { 
    case part ~ Some(negid) => DId(part, negid.toLong) 
    case part ~ None => DId(part) 
  }

  def partition: Parser[Partition] = keyword ^? { case kw @ Keyword(name, Some(Namespace.DB.PART)) => Partition(kw) }
  def dref: Parser[DRef] = (keyword | did) ^? {
    case kw: Keyword => DRef(Left(kw))
    case did: DId => DRef(Right(did))
  }

  def dset: Parser[DSet] = "[" ~> rep(datomicData) <~ "]" ^^ { l => DSet(l.toSet) }

  def druletmp: Parser[(String, Seq[Var])] = "[" ~> functionLiteral ~ rep(variable) <~ "]" ^^ { 
    case name ~ vars => ( name, vars ) }
  def drulealias: Parser[DRuleAlias] = "[" ~> druletmp ~ rep(rule) <~ "]" ^^ {
    case ( name, outputs ) ~ (rules: Seq[_]) => DRuleAlias(name, outputs, rules)
  }
  def drulealiases: Parser[DRuleAliases] = "[" ~> rep(drulealias) <~ "]" ^^ { DRuleAliases(_) }

  def datomicData: Parser[DatomicData] = dset | did | dref | datomicBoolean | datomicString | datomicFloat | datomicLong

  // TERMS
  def datasource: Parser[DataSource] = "$" ~> opt(literal) ^^ {
    case None => ImplicitDS
    case Some(s) => ExternalDS(s)
  }
  def empty: Parser[Term] = "_" ^^ { _ => Empty }
  def keyword: Parser[Keyword] = ":" ~> opt(literal <~ "/") ~ keywordIdent ^^ { 
    case None ~ n => Keyword(n) 
    case Some(ns) ~ n => Keyword(n, Some(Namespace(ns)))
  }

  def posKeyword: Parser[Keyword] = positioned(keyword)

  def variable: Parser[Var] = "?" ~> ident ^^ { (n: String) => Var(n) }
  def const: Parser[Const] = datomicData ^^ { Const(_) }
  def term: Parser[Term] = empty | keyword | variable | const | datasource

  // DATA RULES
  def dataRuleTerms: Parser[(Term, Term, Term, Term, Term)] = ( term ~ term ~ term ~ term ~ term | term ~ term ~ term ~ term | term ~ term ~ term | term ~ term | term ) ^^ {
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) ~ (t4: Term) ~ (t5: Term) => (t1, t2, t3, t4, t5)
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) ~ (t4: Term) => (t1, t2, t3, t4, Empty)
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) => (t1, t2, t3, Empty, Empty)
    case (t1: Term) ~ (t2: Term) => (t1, t2, Empty, Empty, Empty)
    case (t1: Term) => (t1, Empty, Empty, Empty, Empty)
  }

  def dataRule: Parser[DataRule] = opt(datasource) ~ dataRuleTerms ^^ { 
    case None ~ terms => DataRule(ImplicitDS, terms._1, terms._2, terms._3, terms._4, terms._5)
    case Some(ds) ~ terms => DataRule(ds, terms._1, terms._2, terms._3, terms._4, terms._5)
  }

  // EXPRESSION RULES
  def dfunction: Parser[DFunction] = (operator | functionLiteral) ^^ { DFunction(_) }
  def dpredicate: Parser[DPredicate] = (operator | functionLiteral) ^^ { DPredicate(_) }

  def scalarBinding: Parser[ScalarBinding] = (variable | empty) ^^ { ScalarBinding(_) }
  def tupleBinding: Parser[TupleBinding] = "[" ~> rep(variable | empty) <~ "]" ^^ { TupleBinding(_) }
  def collectionBinding: Parser[CollectionBinding] = "[" ~> (variable | empty) <~ "..." ~ "]" ^^ { CollectionBinding(_) }
  def relationBinding: Parser[RelationBinding] = "[[" ~> rep(variable | empty) <~ "]]" ^^ { RelationBinding(_) }

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

  def ruleAliasCall: Parser[RuleAliasCall] = "(" ~> functionLiteral ~ rep(term) <~ ")" ^^ {
    case (name: String) ~ (args: Seq[Term]) => RuleAliasCall(name, args)
  }
  // RULES
  def rule: Parser[Rule] = positioned(("[" ~> (dataRule | expressionRule) <~ "]") | ruleAliasCall)

  // WHERE
  def where: Parser[Where] = positioned(":where" ~> rep(rule) ^^ { Where(_) })

  // IN
  def inDatasource: Parser[InDataSource] = datasource ^^ { InDataSource(_) }
  def inVariable: Parser[InVariable] = binding ^^ { InVariable(_) }
  def inRuleAlias: Parser[InRuleAlias.type] = "%" ^^ { _ => InRuleAlias }
  def input: Parser[reactivedatomic.Input] = inRuleAlias | inDatasource | inVariable
  def in: Parser[In] = positioned(":in" ~> rep(input) ^^ { In(_) })

  // FIND
  def outVariable: Parser[OutVariable] = variable ^^ { OutVariable(_) }
  def output: Parser[Output] = outVariable
  def find: Parser[Find] = positioned(":find" ~> rep(output) ^^ { Find(_) })

  // QUERY
  def query: Parser[PureQuery] = "[" ~> find ~ opt(in) ~ where <~ "]" ^^ { 
    case find ~ in ~ where => PureQuery(find, in, where) 
  }

  def scalaExpr: Parser[ScalaExpr] = "$" ~> ( brackets | literal ) ^^ { ScalaExpr(_) }
  
  def eitherScalaExprOrDatomicData = (scalaExpr | datomicData) ^^ {
    case dd: DatomicData => Right(dd)
    case se: ScalaExpr => Left(se)
  }

  def dSetParsing: Parser[DSetParsing] = "[" ~> rep(eitherScalaExprOrDatomicData) <~ "]" ^^ { DSetParsing(_) } 

  def parsingExpr: Parser[ParsingExpr] = scalaExpr | dSetParsing

  def attribute: Parser[(Keyword, DatomicData)] = keyword ~ datomicData ^^ {
    case kw ~ (dd: DatomicData) => kw -> dd
  }
  def attributeParsing: Parser[(Keyword, Either[ParsingExpr, DatomicData])] = keyword ~ (parsingExpr | datomicData) ^^ {
    case kw ~ (dd: DatomicData) => kw -> Right(dd)
    case kw ~ (se: ParsingExpr) => kw -> Left(se)
  }


  def didParsing: Parser[DIdParsing] = "#db/id" ~> "[" ~> (partition ~ opt(negNoDecimal)) <~ "]" ^^ { 
    case part ~ negid => DIdParsing(part, negid.map(_.toLong)) 
  }

  def drefRestrictedKeyword: Parser[DRef] = keyword ^^ {
    case kw: Keyword => DRef(Left(kw))
  }

  def fact: Parser[Fact] = did ~ keyword ~ (drefRestrictedKeyword | datomicData) ^^ {
    case id ~ kw ~ dd => Fact(id, kw, dd)
  }

  def factParsing: Parser[FactParsing] = (parsingExpr | didParsing) ~ keyword ~ (parsingExpr | drefRestrictedKeyword | datomicData) ^^ {
    case (dd: DIdParsing) ~ kw ~ value => (Right(dd), kw, value)
    case (se: ParsingExpr) ~ kw ~ value => (Left(se), kw, value)
  } ^^ {
    case (id, kw, (dd: DatomicData)) => FactParsing(id, kw, Right(dd))
    case (id, kw, (se: ParsingExpr)) => FactParsing(id, kw, Left(se))
  }

  def addKeyword: Parser[Keyword] = keyword ^? { case kw @ Keyword("add", Some(Namespace.DB)) => kw } 
  def retractKeyword: Parser[Keyword] = keyword ^? { case kw @ Keyword("retract", Some(Namespace.DB)) => kw } 
  def retractEntityKeyword: Parser[Keyword] = keyword ^? { case kw @ Keyword("retractEntity", Some(Namespace.DB)) => kw } 

  def add: Parser[Add] = "[" ~> addKeyword ~> fact <~ "]" ^^ { fact => Add(fact) }
  def addParsing: Parser[AddParsing] = "[" ~> addKeyword ~> factParsing <~ "]" ^^ { fact => AddParsing(fact) }

  def retract: Parser[Retract] = "[" ~> retractKeyword ~> fact <~ "]" ^^ { fact => Retract(fact) }
  def retractParsing: Parser[RetractParsing] = "[" ~> retractKeyword ~> factParsing <~ "]" ^^ { fact => RetractParsing(fact) }

  def retractEntity: Parser[RetractEntity] = "[" ~> retractEntityKeyword ~> datomicLong <~ "]" ^^ { 
    case entid: DLong => RetractEntity(entid) 
  }
  def retractEntityParsing: Parser[RetractEntityParsing] = "[" ~> retractEntityKeyword ~> (parsingExpr | datomicLong) <~ "]" ^^ { 
    case entid: DLong => RetractEntityParsing(Right(entid)) 
    case se: ParsingExpr => RetractEntityParsing(Left(se)) 
  }

  def addToEntity: Parser[AddToEntity] = "{" ~> ensureHasIdKeyword(rep(attribute)) <~ "}"
  def addToEntityParsing: Parser[AddToEntityParsing] = "{" ~> rep(attributeParsing) <~ "}" ^^ { t => AddToEntityParsing(t.toMap) }

  def opsParsing: Parser[Seq[OpParsing]] = "[" ~> rep(addParsing | retractParsing | retractEntityParsing | addToEntityParsing) <~ "]"
  def ops: Parser[Seq[Operation]] = "[" ~> rep(add | retract | retractEntity | addToEntity) <~ "]"

  def eof = """\Z""".r

  def any = {
    Parser(in => if (in.atEnd) {
      Failure("end of file", in)
    } else {
      Success(in.first, in.rest)
    })
  }

  def brackets: Parser[String] = {
    ensureMatchedChars("{", "}")(several((brackets | not("}") ~> any))) ^^ {
      case charList => charList.mkString
    }
  }

  def ensureMatchedChars[T](open: String, close: String)(p: Parser[T]): Parser[T] = Parser { in =>
    val pWithBrackets = open ~> p <~ ( close | eof ~ err("EOF"))
    pWithBrackets(in) match {
      case s @ Success(_, _) => s
      case f @ Failure(_, _) => f
      case Error("EOF", _) => Error("Unmatched bracket", in)
      case e: Error => e
    }
  }

  def ensureHasIdKeyword[T](p: Parser[Seq[(Keyword, DatomicData)]]): Parser[AddToEntity] = Parser { in =>
    p(in) match {
      case Success(t, n) => 
        val attrs = t.toMap
        val idkw = Keyword("id", Namespace.DB)
        attrs.get(idkw) match {
          case Some(id: DId) => Success(AddToEntity(id, attrs - idkw), n)
          case Some(_) => Error("AddToEntity requires at least one DId field", in)
          case None => Error("AddToEntity requires at least one DId field", in)
        }
      case f @ Failure(_, _) => f
      case Error("EOF", _) => Error("Unmatched bracket", in)
      case e: Error => e
    }
  }

  def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
    import scala.collection.mutable.ListBuffer
    val elems = new ListBuffer[T]
    def continue(in: Input): ParseResult[List[T]] = {
      val p0 = p // avoid repeatedly re-evaluating by-name parser
      @tailrec
      def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
        case Success(x, rest) => elems += x; applyp(rest)
        case Failure(_, _) => Success(elems.toList, in0)
        case err: Error => err
      }
      applyp(in)
    }
    continue(in)
  }

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

  def parseAddToEntityParsingSafe(input: String): Either[PositionFailure, AddToEntityParsing] = parseAll(addToEntityParsing, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseOpParsingSafe(input: String): Either[PositionFailure, Seq[OpParsing]] = parseAll(opsParsing, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseOpSafe(input: String): Either[PositionFailure, Seq[Operation]] = parseAll(ops, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

   def parseDRuleAliasesSafe(input: String): Either[PositionFailure, DRuleAliases] = parseAll(drulealiases, input) match {
    case Success(result, _) => Right(result)
    case c @ Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }
}

