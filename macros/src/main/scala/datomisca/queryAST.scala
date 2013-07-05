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

import scala.util.parsing.input.Positional


/* DATOMIC DATA RULES */
case class DataRule(ds: DataSource = ImplicitDS, entity: Term = Empty, attr: Term = Empty, value: Term = Empty, tx: Term = Empty, added: Term = Empty) extends Rule {
  override def toString = """[%s%s%s%s%s%s]""".format(
    if(ds == ImplicitDS) "" else ds+" ",
    if(entity == Empty){ if(attr != Empty || value != Empty || tx != Empty || added != Empty) (entity+" ") else ""} else entity,
    if(attr == Empty){ if(value != Empty || tx != Empty || added != Empty) (" "+attr) else ""} else (" "+attr),
    if(value == Empty){ if(tx != Empty || added != Empty) (" "+value) else "" } else (" "+value),
    if(tx == Empty){ if(added != Empty) (" "+tx) else "" } else (" "+tx),
    if(added == Empty) "" else (" "+added)
  )
}

/* DATOMIC EXPRESSION RULES */
sealed trait Binding
case class ScalarBinding(name: Term) extends Binding {
  override def toString = name.toString
}

case class TupleBinding(names: Seq[Term]) extends Binding {
  override def toString = "[ " + names.map( _.toString ).mkString(" ") + " ]"
}

case class CollectionBinding(name: Term) extends Binding {
  override def toString = "[ " + name.toString + " ... ]" 
}

case class RelationBinding(names: Seq[Term]) extends Binding {
  override def toString = "[[ " + names.map( _.toString ).mkString(" ") + " ]]"
}

case class DFunction(name: String) {
  override def toString = name.toString
}

case class DPredicate(name: String) {
  override def toString = name.toString
}

case class ExpressionRule(expr: Expression) extends Rule {
  override def toString = s"""[ $expr ]"""
}

sealed trait Expression
case class PredicateExpression(predicate: DPredicate, args: Seq[Term]) extends Expression {
  override def toString = s"""($predicate ${args.map( _.toString ).mkString(" ")})"""
}
case class FunctionExpression(function: DFunction, args: Seq[Term], binding: Binding) extends Expression {
  override def toString = s"""($function ${args.map( _.toString ).mkString(" ")}) $binding"""
}

/* RULE ALIAS */
case class RuleAliasCall(name: String, args: Seq[Term]) extends Rule {
  override def toString = """( %s %s )""".format(name, args.map( _.toString ).mkString("", " ", ""))
}

/* WHERE */
case class Where(rules: Seq[Rule]) extends Positional {
  override def toString = rules.map( _.toString ).mkString(":where ", " ", "")
}

/* IN */
case class In(inputs: Seq[Input]) extends Positional {
  override def toString = inputs.map( _.toString ).mkString(":in ", " ", "")
}

sealed trait Input
case class InDataSource(ds: DataSource) extends Input {
  override def toString = ds.toString
}
case class InVariable(binding: Binding) extends Input {
  override def toString = binding.toString
}
case object InRuleAlias extends Input {
  override def toString = "%"
}

/* DATOMIC FIND */
case class Find(outputs: Seq[Output]) extends Positional {
  override def toString = outputs.map( _.toString ).mkString(":find ", " ", "")
}

sealed trait Output
case class OutVariable(variable: Var) extends Output {
  override def toString = variable.toString
}

/* DATOMIC WITH (Optional) */
case class With(variables: Seq[Var]) extends Positional {
  override def toString = variables.map( _.toString ).mkString(":with ", " ", "")
}
