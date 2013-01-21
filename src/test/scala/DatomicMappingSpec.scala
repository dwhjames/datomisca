import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

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
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._

import reactivedatomic._

import Datomic._
import DatomicMapping._

@RunWith(classOf[JUnitRunner])
class DatomicMappingSpec extends Specification {
  sequential

  val uri = "datomic:mem://DatomicMappingSpec"

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Person(name: String, age: Long, birth: java.util.Date, characters: Set[DRef], dog: Option[Ref[Dog]] = None, doggies: Set[Ref[Dog]])
  case class Dog(name: String, age: Long)

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }      

  val dog = Namespace("dog")

  val violent = AddIdent(Keyword(person.character, "violent"))
  val weak = AddIdent(Keyword(person.character, "weak"))
  val clever = AddIdent(Keyword(person.character, "clever"))
  val dumb = AddIdent(Keyword(person.character, "dumb"))
  val stupid = AddIdent(Keyword(person.character, "stupid"))

  object PersonSchema {
    val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
    val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's name")
    val birth = Attribute( person / "birth", SchemaType.instant, Cardinality.one).withDoc("Person's birth date")
    val characters =  Attribute( person / "characters", SchemaType.ref, Cardinality.many).withDoc("Person's characterS")
    val specialChar =  Attribute( person / "specialChar", SchemaType.ref, Cardinality.one).withDoc("Person's Special character")
    val dog =  Attribute( person / "dog", SchemaType.ref, Cardinality.one).withDoc("Person's dog")
    val dogRef =  RefAttribute[Dog]( person / "dog").withDoc("Person's dog")

    val doggies =  ManyRefAttribute[Dog]( person / "doggies").withDoc("Person's doggies")

    val schema = Seq(name, age, birth, characters, specialChar, dog, doggies)
  }

  object DogSchema {
    val name =  Attribute( dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")
    val age =  Attribute( dog / "age", SchemaType.long, Cardinality.one).withDoc("Dog's age")

    val schema = Seq(name, age)
  }

  implicit val dogReader = (
    DogSchema.name.read[String] and
    DogSchema.age.read[Long]
  )(Dog)

  val personReader = (
    PersonSchema.name.read[String] and 
    PersonSchema.age.read[Long] and
    PersonSchema.birth.read[java.util.Date] and
    PersonSchema.characters.read[Set[DRef]] and
    PersonSchema.dog.readOpt[Ref[Dog]] and
    PersonSchema.doggies.read[Set[Ref[Dog]]]
  )(Person)

  val birthDate = new java.util.Date() 
  val medor = Dog("medor", 5L)
  val medorId = DId(Partition.USER)
  var realMedorId = DLong(0L)

  val doggy1 = Dog("doggy1", 5L)
  val doggy1Id = DId(Partition.USER)
  var realDoggy1Id = DLong(0L)

  val doggy2 = Dog("doggy2", 5L)
  val doggy2Id = DId(Partition.USER)
  var realDoggy2Id = DLong(0L)

  val doggy3 = Dog("doggy3", 5L)
  val doggy3Id = DId(Partition.USER)
  var realDoggy3Id = DLong(0L)

  "Datomic" should {
    "create entity" in {
      
      //DatomicBootstrap(uri)
      println("created DB with uri %s: %s".format(uri, createDatabase(uri)))
      implicit val conn = Datomic.connect(uri)

      Await.result(
        transact(PersonSchema.schema ++ DogSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
          println("TX:"+tx)
          Datomic.transact(
            Entity.add(medorId)(
              dog / "name" -> "medor",
              dog / "age" -> 5L
            ),
            Entity.add(doggy1Id)(
              dog / "name" -> "doggy1",
              dog / "age" -> 5L
            ),
            Entity.add(doggy2Id)(
              dog / "name" -> "doggy2",
              dog / "age" -> 5L
            ),
            Entity.add(doggy3Id)(
              dog / "name" -> "doggy3",
              dog / "age" -> 5L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "toto",
              person / "age" -> 30L,
              person / "birth" -> birthDate,
              person / "characters" -> Set(violent, weak),
              person / "specialChar" -> clever,
              person / "dog" -> medorId,
              person / "doggies" -> Set(doggy1Id, doggy2Id, doggy3Id)
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age" -> 54L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age" -> 23L
            )
          ).map{ tx => 
            println("Provisioned data... TX:%s".format(tx))
            tx.resolve(medorId, doggy1Id, doggy2Id, doggy3Id) match{
              case (Some(medorId), Some(doggy1Id), Some(doggy2Id), Some(doggy3Id)) => 
                realMedorId = medorId
                realDoggy1Id = doggy1Id
                realDoggy2Id = doggy2Id
                realDoggy3Id = doggy3Id
              case _ => failure("couldn't resolve IDs")
            }

            Datomic.q(Query.manual[Args0, Args1]("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """)).head match {
              case e: DLong =>
                database.entityOpt(e).map { entity =>
                  println(
                    "dentity age:" + entity.getAs[DLong](person / "age") + 
                    " name:" + entity(person / "name") +
                    " map:" + entity.toMap
                  )
                  DatomicMapping.fromEntity(entity)(personReader).map {
                    case Person(name, age, birth, characters, dog, doggies) => 
                      println(s"Found person with name $name and age $age and birth $birth characters $characters dog $dog doggies $doggies")
                      success
                  }.get
                }.getOrElse(failure("could't find entity"))
              case _ => failure("error")
            }
          }
        },
        Duration("2 seconds")
      )

    }

    "get entity fields from attributes" in {
      import scala.util.{Try, Success, Failure}

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query.manual[Args0, Args1]("""
        [ :find ?e 
          :where [?e :person/name "toto"]
        ]
      """)).head match {
        case e: DLong =>
          database.entityOpt(e).map { entity =>
            val nameValue = entity.get(PersonSchema.name)
            nameValue must beEqualTo(Some("toto"))

            val nameValue2 = entity.as[String](person / "name")
            nameValue2 must beEqualTo("toto")
            
            val nameValue3 = entity.as[DString](person / "name")
            nameValue3.underlying must beEqualTo("toto")

            val ageValue2 = entity.get(PersonSchema.age)
            ageValue2 must beEqualTo(Some(30))

            val ageValue3 = entity.tryGet(PersonSchema.age)
            ageValue3 must beEqualTo(Success(30))

            val ageValue4 = entity.as[Long](person / "age")
            ageValue4 must beEqualTo(30)

            val characters = entity.get(PersonSchema.characters)
            val characters2 = entity.getAs[Set[DRef]](person / "characters")

            val birthValue = entity.as[DInstant](person / "birth")
            birthValue must beEqualTo(DInstant(birthDate))

            val birthValue2 = entity.as[java.util.Date](person / "birth")
            birthValue2 must beEqualTo(birthDate)

            val birthValue3 = entity.get(PersonSchema.birth)
            birthValue3 must beEqualTo(Some(birthDate))

            val dogValue0 = entity.getAs[DEntity](person / "dog")

            val dogValue = entity.getRef[Dog](PersonSchema.dog)
            dogValue must beEqualTo(Some(Ref(DId(realMedorId))(medor)))

            val dogValue2 = entity.get(PersonSchema.dogRef)
            dogValue2 must beEqualTo(Some(Ref(DId(realMedorId))(medor)))

            val doggiesValue = entity.get(PersonSchema.doggies)
            doggiesValue must beEqualTo(Some(Set(
              Ref(DId(realDoggy1Id))(doggy1),
              Ref(DId(realDoggy2Id))(doggy2),
              Ref(DId(realDoggy3Id))(doggy3)
            )))

            val writer = PersonSchema.specialChar.write[DRef]
            writer.write(clever.ident).toMap must beEqualTo(
              PartialAddEntity(Map(
                PersonSchema.specialChar.ident -> clever.ident
              )).toMap
            )

          }.getOrElse(failure("could't find entity"))
        case _ => failure("error")
      }
    }

    "create ops from attributes" in {
      import scala.util.{Try, Success, Failure}

      val id = DId(Partition.USER)
      
      val a = SchemaFact.add(id)( PersonSchema.name -> "toto" )
      a must beEqualTo(AddFact( id, person / "name", DString("toto") ))

      val r = SchemaFact.retract(id)( PersonSchema.name -> "toto" )
      r must beEqualTo(RetractFact( id, person / "name", DString("toto") ))      

      val e = SchemaEntity.add(
        id)(
        Props(PersonSchema.name -> "toto") +
        (PersonSchema.age -> 45L) +
        (PersonSchema.birth -> birthDate) +
        (PersonSchema.characters -> Set(violent, weak))
      )
      e.toString must beEqualTo(AddEntity( 
        id, 
        Map(
          person / "name" -> DString("toto"),
          person / "age" -> DLong(45),
          person / "birth" -> DInstant(birthDate),
          person / "characters" -> DSet(violent.ident, weak.ident)
        )
      ).toString)      

      val props =   
        Props(PersonSchema.name -> "toto") + 
          (PersonSchema.age -> 45L) + 
          (PersonSchema.birth -> birthDate) +
          (PersonSchema.characters -> Set(violent, weak))

      println("Props:"+props)
      //val c = attr2PartialAddToEntityWriterOne[DLong,Long]
      val ageValue = props.get(PersonSchema.age)
      ageValue must beEqualTo(Some(45))

      val ent = SchemaEntity.add(id)(props) 
      ent.toString must beEqualTo(AddEntity( 
        id, 
        Map(
          person / "name" -> DString("toto"),
          person / "age" -> DLong(45),
          person / "birth" -> DInstant(birthDate),
          person / "characters" -> DSet(violent.ident, weak.ident)
        )
      ).toString)
    }
  }
}