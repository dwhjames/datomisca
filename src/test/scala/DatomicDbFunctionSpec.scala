
import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class DatomicDbFunctionSpec extends Specification {
  sequential

  "DatomicDbFunction" should {
    "simplest Fn" in {
      val uri = "datomic:mem://DatomicDbFunctionSpec"

      object Data {
        val foo = AddIdent(KW(":foo"))

        val addDocFn = AddTxFunction(KW(":add-doc"))("db", "e", "doc")("java") {
          """
          return list(list(":db/add", e, ":db/doc", doc));
          """
        }

        val txData = Seq(foo, addDocFn)
      }

      Datomic.createDatabase(uri)
      implicit val conn = connect(uri)

      val maybeRes = Datomic.transact(Data.txData).flatMap{ tx =>
        val fooEntity = database.entity(Data.foo.ident)

        fooEntity(Namespace.DB / "ident") must beEqualTo(DRef(KW(":foo")))

        Datomic.transact(
          InvokeTxFunction(Data.addDocFn.ident)(DRef(Data.foo.ident), DString("this is foo's doc"))
        ).map{ tx =>
          val fooEntity = database.entity(Data.foo.ident)
          //println("fooEntityModif:"+fooEntity.toMap)

          fooEntity(Namespace.DB / "ident") must beEqualTo(DRef(KW(":foo")))
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
              KW(":db/doc") -> note,
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
      implicit val conn = connect(uri)

      Await.result(
        for {
          _ <- transact(AccountsSchema.schema)
          _ <- transact(AccountsTxData.sampleTxData)
        } yield (),
        Duration("3 seconds")
      )

      val issuerId =
        q(AccountsQueries.findAccountByName, database, DString("Issuer")).head.as[DLong]
      val bobId =
        q(AccountsQueries.findAccountByName, database, DString("Bob")).head.as[DLong]
      val aliceId =
        q(AccountsQueries.findAccountByName, database, DString("Alice")).head.as[DLong]

      Await.result(
        for {
          _ <- transact(AccountsTxData.transfer(issuerId, aliceId, BigDecimal(77), "Issuance to Alice"))
          _ =  Thread.sleep(2000)
          _ <- transact(AccountsTxData.transfer(issuerId, bobId,   BigDecimal(23), "Issuance to Bob"))
          _ =  Thread.sleep(2000)
          _ <- transact(AccountsTxData.transfer(aliceId, bobId,    BigDecimal(7),  "Dinner"))
        } yield (),
        Duration("10 seconds")
      )

      //println("bob:"+database.entity(bobId).toMap)
      //println("alice:"+database.entity(aliceId).toMap)

      database.entity(bobId).as[BigDecimal](AccountsSchema.balance.ident) must beEqualTo(30L)
      database.entity(aliceId).as[BigDecimal](AccountsSchema.balance.ident) must beEqualTo(70L)
    }

    "simplest typed Fn" in {
      val uri = "datomic:mem://DatomicDbFunctionSpec"

      object Data {
        val foo = AddIdent(KW(":foo"))

        val addDocFn = AddTxFunction.typed[AddIdent, String](KW(":add-doc"))("db", "e", "doc")("java") {
          """
          return list(list(":db/add", e, ":db/doc", doc));
          """
        }

        val txData = Seq(foo, addDocFn)
      }

      Datomic.createDatabase(uri)
      implicit val conn = connect(uri)

      val maybeRes = Datomic.transact(Data.txData).flatMap{ tx =>
        val fooEntity = database.entity(Data.foo.ident)

        fooEntity(Namespace.DB / "ident") must beEqualTo(DRef(KW(":foo")))

        Datomic.transact(
          InvokeTxFunction(Data.addDocFn)(Data.foo, "this is foo's doc")
        ).map{ tx =>
          val fooEntity = database.entity(Data.foo.ident)
          //println("fooEntityModif:"+fooEntity.toMap)

          fooEntity(Namespace.DB / "ident") must beEqualTo(DRef(KW(":foo")))
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
