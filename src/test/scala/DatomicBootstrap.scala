
import datomisca._
import Datomic._

import scala.language.reflectiveCalls

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object DatomicBootstrap {
  def apply(theUri: String): Future[TxReport] = {
    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }
    val violent = AddIdent(person.character / "violent")
    val weak    = AddIdent(person.character / "weak")
    val clever  = AddIdent(person.character / "clever")
    val dumb    = AddIdent(person.character / "dumb")
    val stupid  = AddIdent(person.character / "stupid")

    val schema = Seq(
      Attribute(person / "name",      SchemaType.string, Cardinality.one) .withDoc("Person's name").withFullText(true),
      Attribute(person / "age",       SchemaType.long,   Cardinality.one) .withDoc("Person's age"),
      Attribute(person / "character", SchemaType.ref,    Cardinality.many).withDoc("Person's characterS"),
      violent,
      weak,
      clever,
      dumb,
      stupid
    )

    println(s"created DB with uri $theUri: ${createDatabase(theUri)}")
    implicit val conn = connect(theUri)

    transact(schema) flatMap { tx =>
      transact(
        Entity.add(DId(Partition.USER))(
          person / "name"      -> "toto",
          person / "age"       -> 30L,
          person / "character" -> Set(weak, dumb)
        ),
        Entity.add(DId(Partition.USER))(
          person / "name"      -> "tutu",
          person / "age"       -> 54L,
          person / "character" -> Set(violent, clever)
        ),
        Entity.add(DId(Partition.USER))(
          person / "name"      -> "tata",
          person / "age"       -> 23L,
          person / "character" -> Set(weak, clever)
        )
      )
    }
  }
}
