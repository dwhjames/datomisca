import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

import datomic.Entity
import datomic.Connection
import datomic.Database
import datomic.Peer
import datomic.Util

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import java.io.Reader
import java.io.FileReader

import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import datomisca._
import Datomic._
import DatomicDataImplicits._

import scala.concurrent.ExecutionContext.Implicits.global


@RunWith(classOf[JUnitRunner])
class DatomicSchemaQuerySpec extends Specification {
  sequential 
  val uri = "datomic:mem://datomicschemaqueryspec"

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }

  val violent = AddIdent(person.character / "violent")
  val weak = AddIdent(Keyword(person.character, "weak"))
  val clever = AddIdent(Keyword(person.character, "clever"))
  val dumb = AddIdent(Keyword(person.character, "dumb"))

  val name = Attribute( KW(":person/name"), SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age = Attribute( KW(":person/age"), SchemaType.long, Cardinality.one).withDoc("Person's age")
  val character = Attribute( KW(":person/character"), SchemaType.ref, Cardinality.many).withDoc("Person's characters")

  val schema = Seq(
    name,
    age,
    character,
    violent,
    weak,
    clever,
    dumb
  )

  def startDB = {
    println("Creating DB with uri %s: %s".format(uri, createDatabase(uri)))

    implicit val conn = Datomic.connect(uri)  
    
    Await.result(
      Datomic.transact(schema).flatMap{ tx => 
        println("Provisioned schema... TX:%s".format(tx))

        val id = DId(Partition.USER)
        Datomic.transact(
          AddEntity(id)(
            Keyword(person, "name") -> DString("toto"),
            Keyword(person, "age") -> DLong(30L),
            Keyword(person, "character") -> DSet(weak.ref, dumb.ref)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tutu"),
            Keyword(person, "age") -> DLong(54L),
            Keyword(person, "character") -> DSet(violent.ref, clever.ref)
          ),
          AddEntity(DId(Partition.USER))(
            Keyword(person, "name") -> DString("tata"),
            Keyword(person, "age") -> DLong(23L),
            Keyword(person, "character") -> DSet(weak.ref, clever.ref)
          )
        ).map{ tx => 
          println("Provisioned data... TX:%s".format(tx))
        }.recover{
          case e => failure(e.getMessage)
        }
      },
      Duration("30 seconds")
    )
  } 

  def stopDB = {
    Datomic.deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic" should {
    "1 - pure query" in {
      implicit val conn = Datomic.connect(uri)
      val query = Query("""
        [ :find ?e ?n 
          :in $ ?char
          :where  [ ?e ${name} ?n ] 
                  [ ?e ${character} ?char ]
        ]
      """)
      
      Datomic.q(
        query, 
        database, 
        DRef(KW(":person.character/violent"))
      ).map {
        case (e: DLong, n: DString) => 
          val entity = database.entity(e)
          println("1 - entity: "+ e + " name:"+n+ " - e:" + entity.get(person / "character"))
      }
      
      success
    }

  }
}