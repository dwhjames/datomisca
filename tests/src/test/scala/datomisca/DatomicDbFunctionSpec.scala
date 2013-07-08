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

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


@RunWith(classOf[JUnitRunner])
class DatomicDbFunctionSpec extends Specification {
  sequential

  "DatomicDbFunction" should {
    "simplest Fn" in {
      val uri = "datomic:mem://DatomicDbFunctionSpec"

      object Data {
        val foo = AddIdent(Datomic.KW(":foo"))

        val addDocFn = AddTxFunction(Datomic.KW(":add-doc"))("db", "e", "doc")("java") {
          """
          return list(list(":db/add", e, ":db/doc", doc));
          """
        }

        val txData = Seq(foo, addDocFn)
      }

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      val maybeRes = Datomic.transact(Data.txData).flatMap{ tx =>
        val fooEntity = Datomic.database.entity(Data.foo.ident)

        fooEntity(Namespace.DB / "ident") must beEqualTo(DKeyword(Datomic.KW(":foo")))

        Datomic.transact(
          InvokeTxFunction(Data.addDocFn.ident)(Data.foo.ref, DString("this is foo's doc"))
        ).map{ tx =>
          val fooEntity = Datomic.database.entity(Data.foo.ident)
          //println("fooEntityModif:"+fooEntity.toMap)

          fooEntity(Namespace.DB / "ident") must beEqualTo(DKeyword(Datomic.KW(":foo")))
          fooEntity(Namespace.DB / "doc") must beEqualTo(DString("this is foo's doc"))

          success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )

      Datomic.deleteDatabase(uri)
    }

    "account Fn" in {

      /*
       * Adapted from https://gist.github.com/pelle/2635666
       */
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
        val from   = Attribute(trans / "from", SchemaType.ref, Cardinality.one)
                       .withDoc("The sending account")
        val to     = Attribute(trans / "to",   SchemaType.ref, Cardinality.one)
                       .withDoc("The receiving account")


        val creditFn = AddTxFunction(fn / "credit") ("db", "id", "amount") ("clojure") {
          """
          (let [ e           (datomic.api/entity db id)
                 min-balance (:account/min-balance e 0)
                 balance     (+ (:account/balance e 0) amount) ]
                 (if (>= balance min-balance)
                   [[:db/add id :account/balance balance]]
                   (throw (Exception. "Insufficient funds"))))
          """
        }
        val transferFn = AddTxFunction(fn / "transfer") ("db", "from", "to", "amount") ("clojure") {
          """
          [
            [:fn/credit from (- amount)]
            [:fn/credit to   amount]
          ]
          """
        }

        // Schema
        val schema = Seq(
          // account, trans, fn,
          name, balance, minBalance,
          amount, from, to,
          creditFn, transferFn
        )

      }

      object AccountsTxData {
        import AccountsSchema._
        val issuer = SchemaEntity.add(DId(Partition.USER))(Props() +
          (AccountsSchema.name       -> "Issuer") +
          (balance    -> BigDecimal(0)) +
          (minBalance -> BigDecimal(-1000))
        )
        val bob = SchemaEntity.add(DId(Partition.USER))(Props() +
          (AccountsSchema.name       -> "Bob") +
          (balance    -> BigDecimal(0)) +
          (minBalance -> BigDecimal(0))
        )
        val alice = SchemaEntity.add(DId(Partition.USER))(Props() +
          (AccountsSchema.name       -> "Alice") +
          (balance    -> BigDecimal(0)) +
          (minBalance -> BigDecimal(0))
        )
        val sampleTxData = Seq(issuer, bob, alice)

        def transfer(fromAcc: DLong, toAcc: DLong, transAmount: BigDecimal, note: String): Seq[Operation] = {
          val txId = DId(Partition.TX)
          Seq(
            InvokeTxFunction(transferFn.ident)(fromAcc, toAcc, DBigDec(transAmount)),
            Entity.add(txId)(
              Datomic.KW(":db/doc") -> note,
              from.ident    -> fromAcc,
              to.ident      -> toAcc,
              amount.ident  -> transAmount
            )
          )
        }

        def credit(toAcc: DLong, amount: BigDecimal): Operation =
          InvokeTxFunction(creditFn.ident)(toAcc, DBigDec(amount))
      }

      object AccountsQueries {
        val queryAccounts = Query("""
          [
            :find ?a
            :in $
            :where
              [?a :account/name]
          ]
        """)

        val findAccountByName = Query("""
          [
            :find ?a
            :in $ ?name
            :where
              [?a :account/name ?name]
          ]
        """)

        val queryAllTransactions = Query("""
          [
            :find ?tx
            :in $
            :where
              [?tx :trans/amount]
          ]
        """)

        val rulesParty = Query.rules("""
          [
            [[party ?t ?a]
               [?t :trans/from ?a]]
            [[party ?t ?a]
               [?t :trans/to ?a]]
          ]
        """)

        val queryAccountTransactions = Query("""
          [
            :find ?t
            :in $ % ?a
            :where
              (party ?t ?a)
          ]
        """)
      }

      val uri = "datomic:mem://DatomicDbFunctionSpec"

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      Await.result(
        for {
          _ <- Datomic.transact(AccountsSchema.schema)
          _ <- Datomic.transact(AccountsTxData.sampleTxData)
        } yield (),
        Duration("3 seconds")
      )

      val issuerId =
        Datomic.q(AccountsQueries.findAccountByName, Datomic.database, DString("Issuer")).head.as[DLong]
      val bobId =
        Datomic.q(AccountsQueries.findAccountByName, Datomic.database, DString("Bob")).head.as[DLong]
      val aliceId =
        Datomic.q(AccountsQueries.findAccountByName, Datomic.database, DString("Alice")).head.as[DLong]

      Await.result(
        for {
          _ <- Datomic.transact(AccountsTxData.transfer(issuerId, aliceId, BigDecimal(77), "Issuance to Alice"))
          _ =  Thread.sleep(2000)
          _ <- Datomic.transact(AccountsTxData.transfer(issuerId, bobId,   BigDecimal(23), "Issuance to Bob"))
          _ =  Thread.sleep(2000)
          _ <- Datomic.transact(AccountsTxData.transfer(aliceId, bobId,    BigDecimal(7),  "Dinner"))
        } yield (),
        Duration("10 seconds")
      )

      //println("bob:"+Datomic.database.entity(bobId).toMap)
      //println("alice:"+Datomic.database.entity(aliceId).toMap)

      Datomic.database.entity(bobId).as[BigDecimal](AccountsSchema.balance.ident) must beEqualTo(30L)
      Datomic.database.entity(aliceId).as[BigDecimal](AccountsSchema.balance.ident) must beEqualTo(70L)
    }

    "simplest typed Fn" in {
      val uri = "datomic:mem://DatomicDbFunctionSpec"

      object Data {
        val foo = AddIdent(Datomic.KW(":foo"))

        val addDocFn = AddTxFunction.typed[AddIdent, String](Datomic.KW(":add-doc"))("db", "e", "doc")("java") {
          """
          return list(list(":db/add", e, ":db/doc", doc));
          """
        }

        val txData = Seq(foo, addDocFn)
      }

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      val maybeRes = Datomic.transact(Data.txData).flatMap{ tx =>
        val fooEntity = Datomic.database.entity(Data.foo.ident)

        fooEntity(Namespace.DB / "ident") must beEqualTo(DKeyword(Datomic.KW(":foo")))

        Datomic.transact(
          InvokeTxFunction(Data.addDocFn)(Data.foo, "this is foo's doc")
        ).map{ tx =>
          val fooEntity = Datomic.database.entity(Data.foo.ident)
          //println("fooEntityModif:"+fooEntity.toMap)

          fooEntity(Namespace.DB / "ident") must beEqualTo(DKeyword(Datomic.KW(":foo")))
          fooEntity(Namespace.DB / "doc") must beEqualTo(DString("this is foo's doc"))

          success
        }

      }

      Await.result(
        maybeRes,
        Duration("3 seconds")
      )

      Datomic.deleteDatabase(uri)
    }
  }
}
