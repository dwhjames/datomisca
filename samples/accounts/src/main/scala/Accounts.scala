
import datomisca._
import Datomic._

import scala.concurrent._
import scala.concurrent.duration.Duration

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


  val creditFn = AddDbFunction(fn / "credit") ("db", "id", "amount") {
    """
    (let [ e           (datomic.api/entity db id)
           min-balance (:account/min-balance e 0)
           balance     (+ (:account/balance e 0) amount) ]
           (if (>= balance min-balance)
             [[:db/add id :account/balance balance]]
             (throw (Exception. "Insufficient funds"))))
    """
  }
  val transferFn = AddDbFunction(fn / "transfer") ("db", "from", "to", "amount") {
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
    (name       -> "Issuer") +
    (balance    -> BigDecimal(0)) +
    (minBalance -> BigDecimal(-1000))
  )
  val bob = SchemaEntity.add(DId(Partition.USER))(Props() +
    (name       -> "Bob") +
    (balance    -> BigDecimal(0)) +
    (minBalance -> BigDecimal(0))
  )
  val alice = SchemaEntity.add(DId(Partition.USER))(Props() +
    (name       -> "Alice") +
    (balance    -> BigDecimal(0)) +
    (minBalance -> BigDecimal(0))
  )
  val sampleTxData = Seq(issuer, bob, alice)

  def transfer(fromAcc: DLong, toAcc: DLong, transAmount: BigDecimal, note: String): Seq[Operation] = {
    val txId = DId(Partition.TX)
    Seq(
      InvokeTxFunction(transferFn.ident, fromAcc, toAcc, DBigDec(transAmount)),
      Entity.add(txId)(
        KW(":db/doc") -> note,
        from.ident    -> fromAcc,
        to.ident      -> toAcc,
        amount.ident  -> transAmount
      )
    )
  }

  def credit(toAcc: DLong, amount: BigDecimal): Operation =
    InvokeTxFunction(creditFn.ident, toAcc, DBigDec(amount))
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

object Accounts {
  // IF RUNNING FROM SBT RUNTIME :
  // This imports a helper Execution Context provided by Datomisca
  // to enhance default Scala one with access to ExecutorService
  // to be able to shut the service down after program execution.
  // Without this shutdown, when running in SBT, at second execution,
  // you get weird Clojure cache execution linked to classloaders issues...
  //
  // IF NOT IN SBT RUNTIME :
  // You can use classic Scala global execution context
  import datomisca.executioncontext.ExecutionContextHelper._

  // Datomic URI definition
  val uri = "datomic:mem://datomisca-accounts-sample"

  // Datomic Connection as an implicit in scope
  implicit lazy val conn = connect(uri)

  def main(args: Array[String]) {
    import AccountsSchema.{name, from, to}
    import AccountsQueries._
    import AccountsTxData.{transfer, credit}
    createDatabase(uri)

    Await.result(
      for {
        _ <- transact(AccountsSchema.schema)
        _ <- transact(AccountsTxData.sampleTxData)
      } yield (),
      Duration("3 seconds")
    )

    val issuerId =
      q(findAccountByName, database, DString("Issuer")).head.as[DLong]
    val bobId =
      q(findAccountByName, database, DString("Bob")).head.as[DLong]
    val aliceId =
      q(findAccountByName, database, DString("Alice")).head.as[DLong]

    Await.result(
      for {
        _ <- transact(transfer(issuerId, aliceId, BigDecimal(77), "Issuance to Alice"))
        _ =  Thread.sleep(2000)
        _ <- transact(transfer(issuerId, bobId,   BigDecimal(23), "Issuance to Bob"))
        _ =  Thread.sleep(2000)
        _ <- transact(transfer(aliceId, bobId,    BigDecimal(7),  "Dinner"))
      } yield (),
      Duration("10 seconds")
    )

    def dumpAcc(eid: DatomicData) {
      val entity = database.entity(eid.asInstanceOf[DLong])
      println(entity.toMap)
    }

    def dumpTx(eid: DatomicData) {
      val entity = database.entity(eid.asInstanceOf[DLong])
      def getName(entity: DEntity, kw: Keyword) =
        entity.as[DEntity](kw).as[DString](name.ident)
      println(
        entity.toMap +
        (from.ident -> getName(entity, from.ident)) +
        (to.ident   -> getName(entity, to.ident))
      )
    }

    println("All accounts")
    q(queryAccounts, database) foreach dumpAcc
    println()

    println("All transactions")
    q(queryAllTransactions, database) foreach dumpTx
    println()

    println("Issuers's account")
    dumpAcc(issuerId)
    println("Issuers's transactions")
    q(queryAccountTransactions, database, rulesParty, issuerId) foreach dumpTx
    println()

    println("Bob's account")
    dumpAcc(bobId)
    println("Bob's transactions")
    q(queryAccountTransactions, database, rulesParty, bobId) foreach dumpTx
    println()

    println("Alice's account")
    dumpAcc(aliceId)
    println("Alice's transactions")
    q(queryAccountTransactions, database, rulesParty, aliceId) foreach dumpTx
    println()

    try {
      Await.result(
        transact(transfer(aliceId, bobId, BigDecimal(71),  "Car")),
        Duration("3 seconds")
      )
    } catch {
      case ex: Throwable =>
        println("A transaction to transfer 71 from Alice to Bob should fail.")
        println(s"It failed with the message ${ex.getMessage}")
    }

    // IF RUNNING FROM SBT RUNTIME :
    // without this, in SBT, if you run the program 2x, it fails
    // with weird cache exception linked to the way SBT manages
    // execution context and classloaders...
    defaultExecutorService.shutdownNow()
  }
}
