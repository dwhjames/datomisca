import reactivedatomic._
import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global

object DatomicBootstrap {
  def apply(theUri: String): Future[TxReport] = {
    import reactivedatomic.Datomic._
    import reactivedatomic._

    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }
    val violent = AddIdent(Keyword(person.character, "violent"))
    val weak = AddIdent(Keyword(person.character, "weak"))
    val clever = AddIdent(Keyword(person.character, "clever"))
    val dumb = AddIdent(Keyword(person.character, "dumb"))
    val stupid = AddIdent(Keyword(person.character, "stupid"))

    val schema = Seq(
      Attribute( Keyword(Namespace("person"), "name"), SchemaType.string, Cardinality.one).withDoc("Person's name").withFullText(true),
      Attribute( Keyword(Namespace("person"), "age"), SchemaType.long, Cardinality.one).withDoc("Person's age"),
      Attribute( Keyword(Namespace("person"), "character"), SchemaType.ref, Cardinality.many).withDoc("Person's characterS"),
      violent,
      weak,
      clever,
      dumb,
      stupid
    )

    println("created DB with uri %s: %s".format(theUri, createDatabase(theUri)))
    implicit val conn = Datomic.connect(theUri) //"datomic:mem://datomicspec2"

    transact(schema).flatMap{ tx =>
      transact(
        AddEntity(DId(Partition.USER))(
          Keyword(person, "name") -> DString("toto"),
          Keyword(person, "age") -> DLong(30L),
          Keyword(person, "character") -> DSet(weak.ident, dumb.ident)
        ),
        AddEntity(DId(Partition.USER))(
          Keyword(person, "name") -> DString("tutu"),
          Keyword(person, "age") -> DLong(54L),
          Keyword(person, "character") -> DSet(violent.ident, clever.ident)
        ),
        AddEntity(DId(Partition.USER))(
          Keyword(person, "name") -> DString("tata"),
          Keyword(person, "age") -> DLong(23L),
          Keyword(person, "character") -> DSet(weak.ident, clever.ident)
        )
      )
    }
  }
}