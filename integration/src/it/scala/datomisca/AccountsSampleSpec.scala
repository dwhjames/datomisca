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

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global


class AccountsSampleSpec
  extends FlatSpec
     with Matchers
     with DatomicFixture
     with AwaitHelper
{

  object AccountsSchema {
    // Namespaces
    val account = new Namespace("account")
    val trans   = new Namespace("trans")
    val fn      = new Namespace("fn")

    // Attributes
    val name       = Attribute(account / "name",        SchemaType.string, Cardinality.one)
                       .withDoc("The name of the account")
                       .withUnique(Unique.value)
                       .withFullText(true)
    val balance    = Attribute(account / "balance",     SchemaType.bigdec, Cardinality.one)
                       .withDoc("The account balance")
    val minBalance = Attribute(account / "min-balance", SchemaType.bigdec, Cardinality.one)
                       .withDoc("The minimum permitted balance for the account")

    val amount = Attribute(trans / "amount", SchemaType.bigdec, Cardinality.one)
                   .withDoc("The transaction amount")
    val from   = Attribute(trans / "from",   SchemaType.ref,    Cardinality.one)
                   .withDoc("The sending account")
    val to     = Attribute(trans / "to",     SchemaType.ref,    Cardinality.one)
                   .withDoc("The receiving account")


    val creditFn = AddTxFunction.typed[Long, BigDecimal](fn / "credit") ("db", "id", "amount") (lang = "clojure", partition = Partition.USER, imports = "", requires = "") {s"""
      (let [e (d/entity db id)
            min-balance (${minBalance} e 0)
            balance (+ (${balance} e 0) amount) ]
            (if (>= balance min-balance)
              [[:db/add id ${balance} balance]]
              (throw (IllegalStateException. "Insufficient funds"))))
    """}

    val transferFn = AddTxFunction.typed[Long, Long, BigDecimal](fn / "transfer") ("db", "from", "to", "amount") (lang = "clojure", partition = Partition.USER, imports = "", requires = "") {s"""
      [[${creditFn.ident} from (- amount)]
       [${creditFn.ident} to   amount]]
    """}

    // Schema
    val schema = Seq(
      name, balance, minBalance,
      amount, from, to,
      creditFn, transferFn
    )
  }


  object AccountsTxData {
    import AccountsSchema._

    val issuer = (
      SchemaEntity.newBuilder
        += (name       -> "Issuer")
        += (balance    -> BigDecimal(0))
        += (minBalance -> BigDecimal(-1000))
    ) withId DId(Partition.USER)

    val bob = (
      SchemaEntity.newBuilder
        += (AccountsSchema.name -> "Bob")
        += (balance             -> BigDecimal(0))
        += (minBalance          -> BigDecimal(0))
    ) withId DId(Partition.USER)

    val alice = (
      SchemaEntity.newBuilder
        += (AccountsSchema.name -> "Alice")
        += (balance             -> BigDecimal(0))
        += (minBalance          -> BigDecimal(0))
    ) withId DId(Partition.USER)

    val sampleTxData = Seq(issuer, bob, alice)

    def transfer(fromAcc: Long, toAcc: Long, transAmount: BigDecimal, note: String): Seq[TxData] = {
      val txId = DId(Partition.TX)
      Seq(
        InvokeTxFunction(transferFn)(fromAcc, toAcc, transAmount),
        Fact.add(txId)(Attribute.doc -> note),
        (SchemaEntity.newBuilder
          += (from   -> fromAcc)
          += (to     -> toAcc)
          += (amount -> transAmount)
        ) withId txId
      )
    }

    def credit(toAcc: Long, amount: BigDecimal): TxData =
      InvokeTxFunction(creditFn)(toAcc, amount)
  }


  object AccountsQueries {
    import AccountsSchema._

    val queryAccounts = Query(s"""
      [:find ?a
       :in $$
       :where [?a ${name}]]
    """)

    val findAccountByName = Query(s"""
      [
        :find ?a
        :in $$ ?name
        :where
          [?a ${name} ?name]
      ]
    """)

    val queryAllTransactions = Query(s"""
      [:find ?tx
       :in $$
       :where
         [?tx ${amount}]]
    """)

    val rulesParty = Query.rules(s"""
      [[[party ?t ?a]
          [?t ${from} ?a]]
       [[party ?t ?a]
          [?t ${to} ?a]]]
    """)

    val queryAccountTransactions = Query("""
      [:find ?t
       :in $ % ?a
       :where
         (party ?t ?a)]
    """)
  }


  "Accounts Sample" should "run to completion" in withDatomicDB { implicit conn =>
    import AccountsSchema.{name, from, to}
    import AccountsQueries._
    import AccountsTxData.{transfer, credit}

    await {
      Datomic.transact(AccountsSchema.schema)
    }
    await {
      Datomic.transact(AccountsTxData.sampleTxData)
    }

    val issuerId =
      Datomic.q(findAccountByName, conn.database(), "Issuer").head.asInstanceOf[Long]
    val bobId =
      Datomic.q(findAccountByName, conn.database(), "Bob").head.asInstanceOf[Long]
    val aliceId =
      Datomic.q(findAccountByName, conn.database(), "Alice").head.asInstanceOf[Long]

    await {
      Datomic.transact(transfer(issuerId, aliceId, BigDecimal(77), "Issuance to Alice"))
    }
    await {
      Datomic.transact(transfer(issuerId, bobId, BigDecimal(23), "Issuance to Bob"))
    }
    await {
      Datomic.transact(transfer(aliceId, bobId, BigDecimal(7), "Dinner"))
    }

    Datomic.q(queryAccounts, conn.database()) should have size (3)

    Datomic.q(queryAllTransactions, conn.database()) should have size (3)

    Datomic.q(queryAccountTransactions, conn.database(), rulesParty, issuerId) should have size (2)

    Datomic.q(queryAccountTransactions, conn.database(), rulesParty, bobId) should have size (2)

    Datomic.q(queryAccountTransactions, conn.database(), rulesParty, aliceId) should have size (2)

    an [IllegalStateException] should be thrownBy {
      await {
        Datomic.transact(transfer(aliceId, bobId, BigDecimal(71),  "Car"))
      }
    }
  }

}
