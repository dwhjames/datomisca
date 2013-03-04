import scala.language.reflectiveCalls

import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration

import datomisca._
import Datomic._
import DatomicMapping._

@RunWith(classOf[JUnitRunner])
class DatomicMappingSpec extends Specification {
  sequential

  val uri = "datomic:mem://DatomicMappingSpec"

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Person(
    name:       String,
    age:        Long,
    birth:      java.util.Date,
    characters: Set[DRef],
    dog:        Option[Ref[Dog]] = None,
    doggies:    Set[Ref[Dog]]
  )

  case class Person2(
    name:       String,
    age:        Long,
    birth:      java.util.Date,
    characters: Set[DRef],
    dog:        Option[Long] = None,
    doggies:    Set[Long]
  )

  case class Dog(name: String, age: Long)

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }      

  val dog = Namespace("dog")

  val violent = AddIdent(person.character / "violent")
  val weak    = AddIdent(person.character / "weak")
  val clever  = AddIdent(person.character / "clever")
  val dumb    = AddIdent(person.character / "dumb")
  val stupid  = AddIdent(person.character / "stupid")

  object PersonSchema {
    val name        = Attribute(person / "name",        SchemaType.string,  Cardinality.one) .withDoc("Person's name")
    val age         = Attribute(person / "age",         SchemaType.long,    Cardinality.one) .withDoc("Person's name")
    val birth       = Attribute(person / "birth",       SchemaType.instant, Cardinality.one) .withDoc("Person's birth date")
    val characters  = Attribute(person / "characters",  SchemaType.ref,     Cardinality.many).withDoc("Person's characters")
    val specialChar = Attribute(person / "specialChar", SchemaType.ref,     Cardinality.one) .withDoc("Person's Special character")
    val dog         = Attribute(person / "dog",         SchemaType.ref,     Cardinality.one) .withDoc("Person's dog")

    val dogRef = RefAttribute[Dog]( person / "dog").withDoc("Person's dog")

    val doggies = ManyRefAttribute[Dog]( person / "doggies").withDoc("Person's doggies")

    val schema = Seq(name, age, birth, characters, specialChar, dog, doggies)
  }

  object DogSchema {
    val name     = Attribute(dog / "name",        SchemaType.string, Cardinality.one).withDoc("Dog's name")
    val fakename = Attribute(person / "fakename", SchemaType.string, Cardinality.one).withDoc("Person's name")
    val age      = Attribute(dog / "age",         SchemaType.long,   Cardinality.one).withDoc("Dog's age")

    val schema = Seq(name, age)
  }

  implicit val dogReader = (
    DogSchema.name.read[String] and
    DogSchema.age .read[Long]
  )(Dog)

  val wrongDogReader = (
    DogSchema.fakename.read[String] and
    DogSchema.age     .read[Long]
  )(Dog)

  val personReader = (
    PersonSchema.name      .read[String]         and
    PersonSchema.age       .read[Long]           and
    PersonSchema.birth     .read[java.util.Date] and
    PersonSchema.characters.read[Set[DRef]]      and
    PersonSchema.dog       .readOpt[Ref[Dog]]    and
    PersonSchema.doggies   .read[Set[Ref[Dog]]]
  )(Person)

  val personReader2 = (
    PersonSchema.name      .read[String]         and
    PersonSchema.age       .read[Long]           and
    PersonSchema.birth     .read[java.util.Date] and
    PersonSchema.characters.read[Set[DRef]]      and
    PersonSchema.dog       .readOpt[Long]        and
    PersonSchema.doggies   .read[Set[Long]]
  )(Person2)

  implicit val personWriter = (
    PersonSchema.name      .write[String]         and
    PersonSchema.age       .write[Long]           and
    PersonSchema.birth     .write[java.util.Date] and
    PersonSchema.characters.write[Set[DRef]]      and
    PersonSchema.dog       .writeOpt[Ref[Dog]]    and
    PersonSchema.doggies   .write[Set[Ref[Dog]]]
  )(unlift(Person.unapply))

  val birthDate = new java.util.Date() 
  val medor = Dog("medor", 5L)
  val medorId = DId(Partition.USER)
  var realMedorId: Long = _

  val doggy1 = Dog("doggy1", 5L)
  val doggy1Id = DId(Partition.USER)
  var realDoggy1Id: Long = _

  val doggy2 = Dog("doggy2", 5L)
  val doggy2Id = DId(Partition.USER)
  var realDoggy2Id: Long = _

  val doggy3 = Dog("doggy3", 5L)
  val doggy3Id = DId(Partition.USER)
  var realDoggy3Id: Long = _


  "Datomic" should {
    "create entity" in {
      
      println(s"created DB with uri $uri: ${createDatabase(uri)}")
      implicit val conn = Datomic.connect(uri)

      Await.result(
        transact(PersonSchema.schema ++ DogSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
          println(s"TX: $tx")
          Datomic.transact(
            Entity.add(medorId)(
              dog / "name" -> "medor",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy1Id)(
              dog / "name" -> "doggy1",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy2Id)(
              dog / "name" -> "doggy2",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy3Id)(
              dog / "name" -> "doggy3",
              dog / "age"  -> 5L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name"        -> "toto",
              person / "age"         -> 30L,
              person / "birth"       -> birthDate,
              person / "characters"  -> Set(violent, weak),
              person / "specialChar" -> clever,
              person / "dog"         -> medorId,
              person / "doggies"     -> Set(doggy1Id, doggy2Id, doggy3Id)
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age"  -> 54L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age"  -> 23L
            )
          ).map{ tx => 
            println(s"Provisioned data... TX: $tx")
            tx.resolve(medorId, doggy1Id, doggy2Id, doggy3Id) match{
              case (medorId, doggy1Id, doggy2Id, doggy3Id) => 
                realMedorId  = medorId
                realDoggy1Id = doggy1Id
                realDoggy2Id = doggy2Id
                realDoggy3Id = doggy3Id
            }

            Datomic.q(Query.manual[Args0, Args1]("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """)).head match {
              case DLong(e) =>
                val entity = database.entity(e)
                println(
                  "dentity age:" + entity.getAs[Long](person / "age") + 
                  " name:" + entity(person / "name") +
                  " map:" + entity.toMap
                )
                val p = DatomicMapping.fromEntity(entity)(personReader)
                val Person(name, age, birth, characters, dog, doggies) = p
                p must beEqualTo(
                  Person("toto", 30L, birthDate, Set(violent.ref, weak.ref), 
                    Some(Ref(DId(realMedorId))(medor)), 
                    Set(Ref(DId(realDoggy1Id))(doggy1), Ref(DId(realDoggy2Id))(doggy2), Ref(DId(realDoggy3Id))(doggy3))
                  )
                )
                println(s"Found person with name $name and age $age and birth $birth characters $characters dog $dog doggies $doggies")
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
        case DLong(e) =>
          val entity = database.entity(e)
          val nameValue = entity.get(PersonSchema.name)
          nameValue must beEqualTo(Some("toto"))

          val nameValue2 = entity.as[String](person / "name")
          nameValue2 must beEqualTo("toto")

          val ageValue2 = entity.get(PersonSchema.age)
          ageValue2 must beEqualTo(Some(30))

          val ageValue3 = entity.tryGet(PersonSchema.age)
          ageValue3 must beEqualTo(Success(30))

          val ageValue4 = entity.as[Long](person / "age")
          ageValue4 must beEqualTo(30)

          val characters = entity.get(PersonSchema.characters)
          val characters2 = entity.getAs[Set[DRef]](person / "characters")

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
          writer.write(clever.ref).toMap must beEqualTo(
            PartialAddEntity(Map(
              PersonSchema.specialChar.ident -> clever.ref
            )).toMap
          )
      }
    }

    "create ops from attributes" in {
      import scala.util.{Try, Success, Failure}

      val id = DId(Partition.USER)
      
      val a = SchemaFact.add(id)( PersonSchema.name -> "toto" )
      a must beEqualTo(AddFact( id, person / "name", DString("toto") ))

      val r = SchemaFact.retract(id)( PersonSchema.name -> "toto" )
      r must beEqualTo(RetractFact( id, person / "name", DString("toto") ))      

      val e = SchemaEntity.add(id)(Props() +
        (PersonSchema.name       -> "toto") +
        (PersonSchema.age        -> 45L) +
        (PersonSchema.birth      -> birthDate) +
        (PersonSchema.characters -> Set(violent, weak))
      )
      e.toString must beEqualTo(AddEntity( 
        id, 
        Map(
          person / "name"       -> DString("toto"),
          person / "age"        -> DLong(45),
          person / "birth"      -> DInstant(birthDate),
          person / "characters" -> DSet(violent.ref, weak.ref)
        )
      ).toString)      

      val props = Props() +
          (PersonSchema.name       -> "toto") + 
          (PersonSchema.age        -> 45L) + 
          (PersonSchema.birth      -> birthDate) +
          (PersonSchema.characters -> Set(violent, weak))

      println(s"Props: $props")

      val ent = SchemaEntity.add(id)(props) 
      ent.toString must beEqualTo(AddEntity( 
        id, 
        Map(
          person / "name"       -> DString("toto"),
          person / "age"        -> DLong(45),
          person / "birth"      -> DInstant(birthDate),
          person / "characters" -> DSet(violent.ref, weak.ref)
        )
      ).toString)
    }

    "get entity with empty set" in {
      
      println(s"created DB with uri $uri: ${createDatabase(uri)}")
      implicit val conn = Datomic.connect(uri)

      Await.result(
        transact(PersonSchema.schema ++ DogSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
          println("TX:"+tx)
          Datomic.transact(
            Entity.add(medorId)(
              dog / "name" -> "medor",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy1Id)(
              dog / "name" -> "doggy1",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy2Id)(
              dog / "name" -> "doggy2",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy3Id)(
              dog / "name" -> "doggy3",
              dog / "age"  -> 5L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name"        -> "toto",
              person / "age"         -> 30L,
              person / "birth"       -> birthDate,
              person / "characters"  -> Set(violent, weak),
              person / "specialChar" -> clever,
              person / "dog"         -> medorId
            )
          ).map{ tx => 
            println(s"Provisioned data... TX: $tx")
            tx.resolve(medorId, doggy1Id, doggy2Id, doggy3Id) match{
              case (medorId, doggy1Id, doggy2Id, doggy3Id) => 
                realMedorId  = medorId
                realDoggy1Id = doggy1Id
                realDoggy2Id = doggy2Id
                realDoggy3Id = doggy3Id
            }

            Datomic.q(Query.manual[Args0, Args1]("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """)).head match {
              case DLong(e) =>
                val entity = database.entity(e)
                println(
                  "dentity age:" + entity.getAs[DLong](person / "age") + 
                  " name:" + entity(person / "name") +
                  " map:" + entity.toMap
                )
                val p = DatomicMapping.fromEntity(entity)(personReader)                
                val Person(name, age, birth, characters, dog, doggies) = p
                println(s"Found person with name $name and age $age and birth $birth characters $characters dog $dog doggies $doggies")
                p must beEqualTo(Person("toto", 30L, birthDate, Set(violent.ref, weak.ref), Some(Ref(DId(realMedorId))(medor)), Set()))
                
                val p2 = DatomicMapping.fromEntity(entity)(personReader2)                
                p2 must beEqualTo(Person2("toto", 30L, birthDate, Set(violent.ref, weak.ref), Some(realMedorId), Set()))

                DatomicMapping.toEntity(DId(e))(
                  Person("toto", 30L, birthDate, Set(violent.ref, weak.ref), 
                    Some(Ref(DId(realMedorId))(medor)), Set())
                ).toMap.get(PersonSchema.doggies.ident) must beEqualTo(None)                

            }
          }
        },
        Duration("2 seconds")
      )

    }

    "entity list" in {      
      println(s"created DB with uri $uri: ${createDatabase(uri)}")
      implicit val conn = Datomic.connect(uri)

      val rd  = PersonSchema.dog.read[Ref[DEntity]]
      val rd2 = PersonSchema.dog.read[DRef]
      val rd3 = PersonSchema.dog.read[Long]

      success
    }
  }

  "EntityReader" should {
    "manage orElse/filter" in {
      implicit val conn = Datomic.connect(uri)

      Await.result(
        transact(PersonSchema.schema ++ DogSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
          println(s"TX: $tx")
          Datomic.transact(
            Entity.add(medorId)(
              dog / "name" -> "medor",
              dog / "age"  -> 5L
            ),
            Entity.add(doggy1Id)(
              dog / "name" -> "doggy1",
              dog / "age"  -> 5L
            )
          ).map{ tx => 
            println(s"Provisioned data... TX: $tx")
            tx.resolve(medorId, doggy1Id) match{
              case (medorId, doggy1Id) => 
                realMedorId  = medorId
                realDoggy1Id = doggy1Id
            }

            DatomicMapping.fromEntity[Dog](database.entity(realMedorId))(wrongDogReader) must throwA[datomisca.EntityKeyNotFoundException] 
            DatomicMapping.fromEntity[Dog](database.entity(realMedorId))(wrongDogReader orElse dogReader) should beEqualTo(Dog("medor", 5L))
            DatomicMapping.fromEntity[Dog](database.entity(realMedorId))(dogReader.filter(dog => dog.name == "medor")) should beEqualTo(Dog("medor", 5L))
            DatomicMapping.fromEntity[Dog](database.entity(realMedorId))(dogReader.filter(dog => dog.name == "brutus")) should throwA[datomisca.EntityMappingException] 
          }
        },
        Duration("2 seconds")
      )  

      success
    }    
  }
}
