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

import scala.language.reflectiveCalls

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
case class AddEntityParsing(props: Map[Keyword, Either[ParsingExpr, DatomicData]]) extends OpParsing
case class FactParsing(id: Either[ParsingExpr, DIdParsing], attr: Keyword, value: Either[ParsingExpr, DatomicData])
case class AddFactParsing(fact: FactParsing) extends OpParsing
case class RetractFactParsing(fact: FactParsing) extends OpParsing
case class RetractEntityParsing(entid: Either[ParsingExpr, DLong]) extends OpParsing

case class TermParsing(value: Either[ScalaExpr, Term]) extends Term {
  override def toString = value match {
    case Left(se) => se.toString
    case Right(t) => t.toString
  }

  def isEmpty = value match {
    case Right(Empty) => true
    case _ => false
  }
}

object TermParsing{
  def apply(t: Term) = new TermParsing(Right(t))
}

case class DataRuleParsing(ds: DataSource, entity: TermParsing, attr: TermParsing, value: TermParsing, tx: TermParsing, added: TermParsing) extends Rule {
  override def toString = """[%s%s%s%s%s%s]""".format(
    if(ds == ImplicitDS) "" else ds+" ",
    if(entity.isEmpty){ if(!attr.isEmpty || !value.isEmpty || !tx.isEmpty || !added.isEmpty) (entity+" ") else ""} else entity,
    if(attr.isEmpty){ if(!value.isEmpty || !tx.isEmpty || !added.isEmpty) (" "+attr) else ""} else (" "+attr),
    if(value.isEmpty){ if(!tx.isEmpty || !added.isEmpty) (" "+value) else "" } else (" "+value),
    if(tx.isEmpty){ if(!added.isEmpty) (" "+tx) else "" } else (" "+tx),
    if(added.isEmpty) "" else (" "+added)
  )
}

object DataRuleParsing{
  def apply(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty, tx: Term = Empty, added: Term = Empty) = 
    new DataRuleParsing(ds, TermParsing(entity), TermParsing(attr), TermParsing(value), TermParsing(tx), TermParsing(added))
}

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

  val functionLiteral = """[a-zA-Z.]([a-zA-Z0-9.]|(-|_|/)[a-zA-Z0-9.])*""".r
  val operator: Parser[String] = """(\+|-|\*|/|not=|==|=|<=|>=|<|>)""".r
  //val operator: Parser[String] = "+" | "-" | "*" | "/" | "==" | "<=" | ">=" | "<" | ">"

  def wrap[T](left: String, parser: Parser[T], right: String): Parser[T] = left ~> parser <~ right

  def parens[T](parser:   Parser[T]): Parser[T] = wrap("(", parser, ")")
  def brackets[T](parser: Parser[T]): Parser[T] = wrap("[", parser, "]")
  def braces[T](parser:   Parser[T]): Parser[T] = wrap("{", parser, "}")
  def dquotes[T](parser:  Parser[T]): Parser[T] = wrap("\"", parser, "\"")

  def parensOrBrackets[T](parser: Parser[T]): Parser[T] = parens(parser) | brackets(parser)

  def datomicString: Parser[DString] = dquotes(stringContent) ^^ { DString(_) }
  def datomicLong: Parser[DLong] = noDecimal ^^ { (s: String) => DLong(s.toLong) }
  def datomicFloat: Parser[DFloat] = decimalMandatory ^^ {  (s: String) => DFloat(s.toFloat) }
  //def datomicDouble: Parser[DDouble] = floatingPointNumber ^^ { (s: String) => DDouble(s.toDouble) }
  def datomicBoolean: Parser[DBoolean] = ("true" | "false") ^^ {  (s: String) => DBoolean(s.toBoolean) }

  def did: Parser[DId] = "#db/id" ~> brackets(partition ~ opt(negNoDecimal)) ^^ { 
    case part ~ Some(negid) => DId(part, negid.toLong) 
    case part ~ None => DId(part) 
  }

  def partition: Parser[Partition] = keyword ^? { case kw @ Keyword(name, Some(Namespace.DB.PART)) => Partition(kw) }
  def dref: Parser[DRef] = (keyword | did) ^? {
    case kw: Keyword => DRef(kw)
    case did: DId => DRef(did)
  }

  def dset: Parser[DSet] = brackets(rep(datomicData)) ^^ { l => DSet(l.toSet) }

  def druletmp: Parser[(String, Seq[Var])] = parensOrBrackets(functionLiteral ~ rep(variable)) ^^ { 
    case name ~ vars => ( name, vars ) }
  def drulealias: Parser[DRuleAlias] = brackets(druletmp ~ rep(rule)) ^^ {
    case ( name, outputs ) ~ (rules: Seq[_]) => DRuleAlias(name, outputs, rules)
  }
  def drulealiases: Parser[DRuleAliases] = brackets(rep(drulealias)) ^^ { DRuleAliases(_) }

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
  def termParsing: Parser[TermParsing] = (scalaExpr | term) ^^ {
    case t: Term => TermParsing(Right(t))
    case se: ScalaExpr => TermParsing(Left(se))
  } 

  // DATA RULES
  def dataRuleTerms: Parser[(Term, Term, Term, Term, Term)] = ( term ~ term ~ term ~ term ~ term | term ~ term ~ term ~ term | term ~ term ~ term | term ~ term | term ) ^^ {
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) ~ (t4: Term) ~ (t5: Term) => (t1, t2, t3, t4, t5)
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) ~ (t4: Term) => (t1, t2, t3, t4, Empty)
    case (t1: Term) ~ (t2: Term) ~ (t3: Term) => (t1, t2, t3, Empty, Empty)
    case (t1: Term) ~ (t2: Term) => (t1, t2, Empty, Empty, Empty)
    case (t1: Term) => (t1, Empty, Empty, Empty, Empty)
  }

  def dataRuleTermParsings: Parser[(TermParsing, TermParsing, TermParsing, TermParsing, TermParsing)] = ( termParsing ~ termParsing ~ termParsing ~ termParsing ~ termParsing | termParsing ~ termParsing ~ termParsing ~ termParsing | termParsing ~ termParsing ~ termParsing | termParsing ~ termParsing | termParsing ) ^^ {
    case (t1: TermParsing) ~ (t2: TermParsing) ~ (t3: TermParsing) ~ (t4: TermParsing) ~ (t5: TermParsing) => (t1, t2, t3, t4, t5)
    case (t1: TermParsing) ~ (t2: TermParsing) ~ (t3: TermParsing) ~ (t4: TermParsing) => (t1, t2, t3, t4, TermParsing(Right(Empty)))
    case (t1: TermParsing) ~ (t2: TermParsing) ~ (t3: TermParsing) => (t1, t2, t3, TermParsing(Right(Empty)), TermParsing(Right(Empty)))
    case (t1: TermParsing) ~ (t2: TermParsing) => (t1, t2, TermParsing(Right(Empty)), TermParsing(Right(Empty)), TermParsing(Right(Empty)))
    case (t1: TermParsing) => (t1, TermParsing(Right(Empty)), TermParsing(Right(Empty)), TermParsing(Right(Empty)), TermParsing(Right(Empty)))
  }

  def dataRule: Parser[DataRule] = opt(datasource) ~ dataRuleTerms ^^ { 
    case None ~ terms => DataRule(ImplicitDS, terms._1, terms._2, terms._3, terms._4, terms._5)
    case Some(ds) ~ terms => DataRule(ds, terms._1, terms._2, terms._3, terms._4, terms._5)
  }

  def dataRuleParsing: Parser[DataRuleParsing] = opt(datasource) ~ dataRuleTermParsings ^^ { 
    case None ~ terms => DataRuleParsing(ImplicitDS, terms._1, terms._2, terms._3, terms._4, terms._5)
    case Some(ds) ~ terms => DataRuleParsing(ds, terms._1, terms._2, terms._3, terms._4, terms._5)
  }

  // EXPRESSION RULES
  def dfunction: Parser[DFunction] = (operator | functionLiteral) ^^ { DFunction(_) }
  def dpredicate: Parser[DPredicate] = (operator | functionLiteral) ^^ { DPredicate(_) }

  def scalarBinding: Parser[ScalarBinding] = (variable | empty) ^^ { ScalarBinding(_) }
  def tupleBinding: Parser[TupleBinding] = brackets(rep(variable | empty)) ^^ { TupleBinding(_) }
  def collectionBinding: Parser[CollectionBinding] = brackets((variable | empty) <~ "...") ^^ { CollectionBinding(_) }
  def relationBinding: Parser[RelationBinding] = wrap("[[", rep(variable | empty), "]]") ^^ { RelationBinding(_) }

  def binding = tupleBinding | scalarBinding | relationBinding | collectionBinding

  def functionAloneExpression: Parser[DFunction ~ Seq[Term]] = parens(dfunction ~ rep(term))

  def functionExpression: Parser[FunctionExpression] = functionAloneExpression ~ binding ^^ {
    case ((df: DFunction) ~ (args: Seq[Term])) ~ (binding: Binding) => FunctionExpression(df, args, binding)
  }

  def predicateExpression: Parser[PredicateExpression] = parens(dpredicate ~ rep(term)) ^^ {
    case (df: DPredicate) ~ (args: Seq[_]) => PredicateExpression(df, args)
  }

  def expression: Parser[Expression] = functionExpression | predicateExpression
  def expressionRule: Parser[ExpressionRule] = expression ^^ { ExpressionRule(_) }

  def ruleAliasCall: Parser[RuleAliasCall] = parens(functionLiteral ~ rep(term)) ^^ {
    case (name: String) ~ (args: Seq[Term]) => RuleAliasCall(name, args)
  }
  // RULES
  def rule: Parser[Rule] = positioned(brackets(dataRuleParsing | expressionRule) | ruleAliasCall)

  // WHERE
  def where: Parser[Where] = positioned(":where" ~> rep(rule) ^^ { Where(_) })

  // IN
  def inDatasource: Parser[InDataSource] = datasource ^^ { InDataSource(_) }
  def inVariable: Parser[InVariable] = binding ^^ { InVariable(_) }
  def inRuleAlias: Parser[InRuleAlias.type] = "%" ^^ { _ => InRuleAlias }
  def input: Parser[datomisca.Input] = inRuleAlias | inDatasource | inVariable
  def in: Parser[In] = positioned(":in" ~> rep(input) ^^ { In(_) })

  // FIND
  def outVariable: Parser[OutVariable] = variable ^^ { OutVariable(_) }
  def output: Parser[Output] = outVariable
  def find: Parser[Find] = positioned(":find" ~> rep(output) ^^ { Find(_) })

  // WITH
  def wizz: Parser[With] = positioned(":with" ~> rep(variable) ^^ { With(_) })

  // QUERY
  def query: Parser[PureQuery] = brackets(find ~ opt(wizz) ~ opt(in) ~ where) ^^ { 
    case find ~ wizz ~ in ~ where => PureQuery(find, wizz, in, where) 
  }

  def scalaExpr: Parser[ScalaExpr] = "$" ~> ( brackets | literal ) ^^ { ScalaExpr(_) }
  
  def eitherScalaExprOrDatomicData = (scalaExpr | datomicData) ^^ {
    case dd: DatomicData => Right(dd)
    case se: ScalaExpr => Left(se)
  }

  def dSetParsing: Parser[DSetParsing] = brackets(rep(eitherScalaExprOrDatomicData)) ^^ { DSetParsing(_) } 

  def parsingExpr: Parser[ParsingExpr] = scalaExpr | dSetParsing

  def attribute: Parser[(Keyword, DatomicData)] = keyword ~ datomicData ^^ {
    case kw ~ (dd: DatomicData) => kw -> dd
  }
  def attributeParsing: Parser[(Keyword, Either[ParsingExpr, DatomicData])] = keyword ~ (parsingExpr | datomicData) ^^ {
    case kw ~ (dd: DatomicData) => kw -> Right(dd)
    case kw ~ (se: ParsingExpr) => kw -> Left(se)
  }


  def didParsing: Parser[DIdParsing] = "#db/id" ~> brackets(partition ~ opt(negNoDecimal)) ^^ { 
    case part ~ negid => DIdParsing(part, negid.map(_.toLong)) 
  }

  def drefRestrictedKeyword: Parser[DRef] = keyword ^^ {
    case kw: Keyword => DRef(kw)
  }

  def factWithTempId: Parser[Fact] = did ~ keyword ~ (drefRestrictedKeyword | datomicData) ^^ {
    case id ~ kw ~ dd => Fact(id, kw, dd)
  }

  def factWithFinalId: Parser[Fact] = datomicLong ~ keyword ~ (drefRestrictedKeyword | datomicData) ^^ {
    case id ~ kw ~ dd => Fact(DId(id), kw, dd)
  }

  def fact: Parser[Fact] = factWithTempId | factWithFinalId

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

  def add: Parser[AddFact] = brackets(addKeyword ~> fact) ^^ { fact => AddFact(fact) }
  def addParsing: Parser[AddFactParsing] = brackets(addKeyword ~> factParsing) ^^ { fact => AddFactParsing(fact) }

  def retract: Parser[RetractFact] = brackets(retractKeyword ~> factWithFinalId) ^^ { fact => RetractFact(fact.id.asInstanceOf[FinalId].underlying, fact) }
  def retractParsing: Parser[RetractFactParsing] = brackets(retractKeyword ~> factParsing) ^^ { fact => RetractFactParsing(fact) }

  def retractEntity: Parser[RetractEntity] = brackets(retractEntityKeyword ~> datomicLong) ^^ { 
    case entid: DLong => RetractEntity(entid) 
  }
  def retractEntityParsing: Parser[RetractEntityParsing] = brackets(retractEntityKeyword ~> (parsingExpr | datomicLong)) ^^ { 
    case entid: DLong => RetractEntityParsing(Right(entid)) 
    case se: ParsingExpr => RetractEntityParsing(Left(se)) 
  }

  def addEntity: Parser[AddEntity] = braces(ensureHasIdKeyword(rep(attribute)))
  def addEntityParsing: Parser[AddEntityParsing] = braces(rep(attributeParsing)) ^^ { t => AddEntityParsing(t.toMap) }

  def opsParsing: Parser[Seq[OpParsing]] = brackets(rep(addParsing | retractParsing | retractEntityParsing | addEntityParsing))
  def ops: Parser[Seq[Operation]] = rep(brackets(rep(add | retract | retractEntity | addEntity))) ^^ {
    case l: Seq[Seq[Operation]] => l.flatten
  }

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

  def ensureHasIdKeyword[T](p: Parser[Seq[(Keyword, DatomicData)]]): Parser[AddEntity] = Parser { in =>
    p(in) match {
      case Success(t, n) => 
        val attrs = t.toMap
        val idkw = Keyword("id", Namespace.DB)
        attrs.get(idkw) match {
          case Some(id: DId) => Success(AddEntity(id, attrs - idkw), n)
          case Some(_) => Error("AddEntity requires at least one DId field", in)
          case None => Error("AddEntity requires at least one DId field", in)
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
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
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
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseAddEntityParsingSafe(input: String): Either[PositionFailure, AddEntityParsing] = parseAll(addEntityParsing, input) match {
    case Success(result, _) => Right(result)
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseOpParsingSafe(input: String): Either[PositionFailure, Seq[OpParsing]] = parseAll(opsParsing, input) match {
    case Success(result, _) => Right(result)
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

  def parseOpSafe(input: String): Either[PositionFailure, Seq[Operation]] = parseAll(ops, input) match {
    case Success(result, _) => Right(result)
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }

   def parseDRuleAliasesSafe(input: String): Either[PositionFailure, DRuleAliases] = parseAll(drulealiases, input) match {
    case Success(result, _) => Right(result)
    case Failure(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
    case Error(msg, input) => Left(PositionFailure(msg, input.pos.line, input.pos.column))
  }
}

