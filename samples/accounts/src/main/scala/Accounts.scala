
import datomisca._

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


  val creditFn = AddTxFunction(fn / "credit") ("db", "id", "amount") ("clojure") {
    """
    (let [ e           (datomic.api/entity db id)
           min-balance (:account/min-balance e 0)
           balance     (+ (:account/balance e 0) amount) ]
           (if (>= balance min-balance)
             [[:db/add id :account/balance balance]]
             (throw (java.lang.IllegalStateException. "Insufficient funds"))))
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
      InvokeTxFunction(transferFn.ident)(fromAcc: java.lang.Long, toAcc: java.lang.Long, transAmount.bigDecimal),
      Entity.add(txId)(
        Datomic.KW(":db/doc") -> note,
        from.ident    -> fromAcc,
        to.ident      -> toAcc,
        amount.ident  -> transAmount
      )
    )
  }

  def credit(toAcc: Long, amount: BigDecimal): TxData =
    InvokeTxFunction(creditFn.ident)(toAcc: java.lang.Long, amount.bigDecimal)
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
  implicit lazy val conn = Datomic.connect(uri)

  def main(args: Array[String]) {
    import AccountsSchema.{name, from, to}
    import AccountsQueries._
    import AccountsTxData.{transfer, credit}
    Datomic.createDatabase(uri)

    Await.result(
      for {
        _ <- Datomic.transact(AccountsSchema.schema)
        _ <- Datomic.transact(AccountsTxData.sampleTxData)
      } yield (),
      Duration("3 seconds")
    )

    val issuerId =
      Datomic.q(findAccountByName, Datomic.database, "Issuer").head.asInstanceOf[Long]
    val bobId =
      Datomic.q(findAccountByName, Datomic.database, "Bob").head.asInstanceOf[Long]
    val aliceId =
      Datomic.q(findAccountByName, Datomic.database, "Alice").head.asInstanceOf[Long]

    Await.result(
      for {
        _ <- Datomic.transact(transfer(issuerId, aliceId, BigDecimal(77), "Issuance to Alice"))
        _ =  Thread.sleep(2000)
        _ <- Datomic.transact(transfer(issuerId, bobId,   BigDecimal(23), "Issuance to Bob"))
        _ =  Thread.sleep(2000)
        _ <- Datomic.transact(transfer(aliceId, bobId,    BigDecimal(7),  "Dinner"))
      } yield (),
      Duration("10 seconds")
    )

    val dumpAcc: (Any => Unit) = {
      case eid: Long =>
        val entity = Datomic.database.entity(eid)
        println(entity.toMap)
    }

    val dumpTx: (Any => Unit) = {
      case eid: Long =>
        val entity = Datomic.database.entity(eid)
        def getName(entity: Entity, kw: Keyword) =
          entity.as[Entity](kw).as[String](name.ident)
        println(
          entity.toMap +
          (from.ident.toString -> getName(entity, from.ident)) +
          (to.ident.toString   -> getName(entity, to.ident))
        )
    }

    println("All accounts")
    Datomic.q(queryAccounts, Datomic.database) foreach dumpAcc
    println()

    println("All transactions")
    Datomic.q(queryAllTransactions, Datomic.database) foreach dumpTx
    println()

    println("Issuers's account")
    dumpAcc(issuerId)
    println("Issuers's transactions")
    Datomic.q(queryAccountTransactions, Datomic.database, rulesParty, issuerId) foreach dumpTx
    println()

    println("Bob's account")
    dumpAcc(bobId)
    println("Bob's transactions")
    Datomic.q(queryAccountTransactions, Datomic.database, rulesParty, bobId) foreach dumpTx
    println()

    println("Alice's account")
    dumpAcc(aliceId)
    println("Alice's transactions")
    Datomic.q(queryAccountTransactions, Datomic.database, rulesParty, aliceId) foreach dumpTx
    println()

    try {
      Await.result(
        Datomic.transact(transfer(aliceId, bobId, BigDecimal(71),  "Car")),
        Duration("3 seconds")
      )
    } catch {
      case ex: java.lang.IllegalStateException =>
        println("A transaction to transfer 71 from Alice to Bob should fail.")
        println(s"It failed with the message ${ex.getMessage}")
      case ex: Throwable =>
        println(s"Unexpected exception $ex")
    }

    Datomic.shutdown(true)

    // IF RUNNING FROM SBT RUNTIME :
    // without this, in SBT, if you run the program 2x, it fails
    // with weird cache exception linked to the way SBT manages
    // execution context and classloaders...
    defaultExecutorService.shutdownNow()
  }
}
