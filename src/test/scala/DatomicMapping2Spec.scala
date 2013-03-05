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
class DatomicMapping2Spec extends Specification {
  sequential

  val uri = "datomic:mem://DatomicMapping2Spec"

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Person(id: Long, name: String, age: Long, birth: java.util.Date, characters: Set[DRef], specialChar: DRef, dog: Option[Dog] = None, doggies: Set[Dog])
  case class Dog(id: Option[Long], name: String, age: Long)

  case class Person2(
    id: Long, name: String, age: Long, birth: java.util.Date, 
    characters: Set[DRef], specialChar: DRef, 
    dog: Option[Long] = None, doggies: Set[Long])

  case class Person3(
    id: Long, name: String, age: Long, birth: java.util.Date, 
    characters: Set[DRef], specialChar: DRef, 
    dog: Option[Long] = None, doggies: Option[Set[Long]] = None)

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
    val age         = Attribute(person / "age",         SchemaType.long,    Cardinality.one) .withDoc("Person's age")
    val birth       = Attribute(person / "birth",       SchemaType.instant, Cardinality.one) .withDoc("Person's birth date")
    val characters  = Attribute(person / "characters",  SchemaType.ref,     Cardinality.many).withDoc("Person's characters")
    val specialChar = Attribute(person / "specialChar", SchemaType.ref,     Cardinality.one) .withDoc("Person's Special character")
    val dog         = Attribute(person / "dog",         SchemaType.ref,     Cardinality.one) .withDoc("Person's dog")
    
    val dogRef = RefAttribute[Dog]( person / "dog").withDoc("Person's dog")

    val doggies =  ManyRefAttribute[Dog]( person / "doggies").withDoc("Person's doggies")

    val schema = Seq(name, age, birth, characters, specialChar, dog, doggies)
  }

  object DogSchema {
    val name = Attribute(dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")
    val age  = Attribute(dog / "age",  SchemaType.long,   Cardinality.one).withDoc("Dog's age")

    val schema = Seq(name, age)
  }

  implicit val dogReader = (
    DatomicMapping.readIdOpt and
    DogSchema.name.read[String] and
    DogSchema.age .read[Long]
  )(Dog)

  /*implicit val dogWriter = (
    DatomicMapping.writeIdOpt and
    DogSchema.name.write[String] and
    DogSchema.age.write[Long]
  )(unlift(Dog.unapply))*/

  implicit val personReader = (
    DatomicMapping.readId and
    PersonSchema.name       .read[String]         and 
    PersonSchema.age        .read[Long]           and
    PersonSchema.birth      .read[java.util.Date] and
    PersonSchema.characters .read[Set[DRef]]      and
    PersonSchema.specialChar.read[DRef]           and
    PersonSchema.dog        .readOpt[Dog]         and
    PersonSchema.doggies    .read[Set[Dog]]
  )(Person)

  implicit val person2Reader = (
    DatomicMapping.readId and
    PersonSchema.name       .read[String]         and 
    PersonSchema.age        .read[Long]           and
    PersonSchema.birth      .read[java.util.Date] and
    PersonSchema.characters .read[Set[DRef]]      and
    PersonSchema.specialChar.read[DRef]           and
    PersonSchema.dog        .readOpt[Long]        and
    PersonSchema.doggies    .read[Set[Long]]
  )(Person2)

  implicit val person3Reader = (
    DatomicMapping.readId and
    PersonSchema.name       .read[String]         and 
    PersonSchema.age        .read[Long]           and
    PersonSchema.birth      .read[java.util.Date] and
    PersonSchema.characters .read[Set[DRef]]      and
    PersonSchema.specialChar.read[DRef]           and
    PersonSchema.dog        .readOpt[Long]        and
    PersonSchema.doggies    .readOpt[Set[Long]]
  )(Person3)

/*implicit val personWriter = (
    DatomicMapping.writeId and
    PersonSchema.name       .write[String]         and 
    PersonSchema.age        .write[Long]           and
    PersonSchema.birth      .write[java.util.Date] and
    PersonSchema.characters .write[Set[DRef]]      and
    PersonSchema.specialChar.write[DRef]           and
    PersonSchema.dog        .writeOpt[Dog]         and
    PersonSchema.doggies    .write[Set[Dog]]
  )(unlif(Person.unapply))*/


  val birthDate = new java.util.Date() 
  val medor = Dog(None, "medor", 5L)
  val medorId = DId(Partition.USER)
  var realMedorId: Long = _

  val doggy1 = Dog(None, "doggy1", 5L)
  val doggy1Id = DId(Partition.USER)
  var realDoggy1Id: Long = _

  val doggy2 = Dog(None, "doggy2", 5L)
  val doggy2Id = DId(Partition.USER)
  var realDoggy2Id: Long = _

  val doggy3 = Dog(None, "doggy3", 5L)
  val doggy3Id = DId(Partition.USER)
  var realDoggy3Id: Long = _

  val toto = Person(
    0L, "toto", 30L, birthDate, 
    Set(violent.ref, weak.ref), clever.ref, Some(medor), Set(doggy1, doggy2, doggy3)
  )
  val totobis = Person2(
    0L, "toto", 30L, birthDate, 
    Set(violent.ref, weak.ref), clever.ref, None, Set()
  )
  val totoId = DId(Partition.USER)
  var realTotoId: Long = _

  val toto2 = Person3(
    0L, "toto2", 30L, birthDate, 
    Set(violent.ref, weak.ref), clever.ref, None, None
  )
  val toto2Id = DId(Partition.USER)
  var realToto2Id: Long = _

  "Datomic" should {
    "create entity" in {
      
      //DatomicBootstrap(uri)
      println(s"created DB with uri $uri: ${Datomic.createDatabase(uri)}")
      implicit val conn = Datomic.connect(uri)

      Await.result(
        Datomic.transact(PersonSchema.schema ++ DogSchema.schema ++ Seq(violent, weak, dumb, clever, stupid)).flatMap{ tx =>
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
            Entity.add(totoId)(
              person / "name"        -> "toto",
              person / "age"         -> 30L,
              person / "birth"       -> birthDate,
              person / "characters"  -> Set(violent, weak),
              person / "specialChar" -> clever,
              person / "dog"         -> medorId,
              person / "doggies"     -> Set(doggy1Id, doggy2Id, doggy3Id)
            ),
            Entity.add(toto2Id)(
              person / "name"        -> "toto2",
              person / "age"         -> 30L,
              person / "birth"       -> birthDate,
              person / "characters"  -> Set(violent, weak),
              person / "specialChar" -> clever
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tutu",
              person / "age"  -> 54L
            ),
            Entity.add(DId(Partition.USER))(
              person / "name" -> "tata",
              person / "age"  -> 23L
            )
          ) map { tx => 
            println(s"Provisioned data... TX: $tx")
            realTotoId   = tx.resolve(totoId)
            realToto2Id  = tx.resolve(toto2Id)
            realMedorId  = tx.resolve(medorId)
            realDoggy1Id = tx.resolve(doggy1Id)
            realDoggy2Id = tx.resolve(doggy2Id)
            realDoggy3Id = tx.resolve(doggy3Id)

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
                val Person(id, name, age, birth, characters, specialChar, dog, doggies) = DatomicMapping.fromEntity(entity)(personReader)
                println(s"Found person with id $id name $name and age $age and birth $birth characters $characters specialChar $specialChar dog $dog doggies $doggies")
            }
          }
        },
        Duration("2 seconds")
      )

    }

    "read case class with ID" in {
      import scala.util.{Try, Success, Failure}

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query.manual[Args0, Args1]("""
        [ :find ?e 
          :where [?e :dog/name "medor"]
        ]
      """)).head match {
        case DLong(e) =>
          val entity = database.entity(e)
          DatomicMapping.fromEntity[Dog](entity) must beEqualTo(medor.copy(id=Some(realMedorId)))
      }

      Datomic.q(Query.manual[Args0, Args1]("""
        [ :find ?e 
          :where [?e :person/name "toto"]
        ]
      """)).head match {
        case DLong(e) =>
          val entity = database.entity(e)
          val realMedor = medor.copy(id=Some(realMedorId))
          val realDoggy1 = doggy1.copy(id=Some(realDoggy1Id))
          val realDoggy2 = doggy2.copy(id=Some(realDoggy2Id))
          val realDoggy3 = doggy3.copy(id=Some(realDoggy3Id))
          
          DatomicMapping.fromEntity[Person](entity) must beEqualTo(
            toto.copy(
              id      = realTotoId,
              dog     = Some(realMedor),
              doggies = Set(realDoggy1, realDoggy2, realDoggy3)
            ))

          DatomicMapping.fromEntity[Person2](entity) must beEqualTo(
            totobis.copy(
              id      = realTotoId,
              dog     = Some(realMedorId),
              doggies = Set(realDoggy1Id, realDoggy2Id, realDoggy3Id)
            ))
      }

      Datomic.q(Query.manual[Args0, Args1]("""
        [ :find ?e 
          :where [?e :person/name "toto2"]
        ]
      """)).head match {
        case DLong(e) =>
          val entity = database.entity(e)
          DatomicMapping.fromEntity[Person3](entity) must beEqualTo(
            toto2.copy(
              id=realToto2Id
            ))
      }
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

          entity(PersonSchema.name) must beEqualTo("toto")

          entity.get(PersonSchema.name) must beEqualTo(Some("toto"))

          entity.as[String](person / "name") must beEqualTo("toto")

          entity.as[Long](person / "age") must beEqualTo(30)

          val characters = entity.get(PersonSchema.characters)
          val characters2 = entity.getAs[Set[DRef]](person / "characters")

          entity.as[java.util.Date](person / "birth") must beEqualTo(birthDate)

          entity.get(PersonSchema.birth) must beEqualTo(Some(birthDate))

          val dogValue0 = entity.getAs[DEntity](person / "dog")

          entity.getRef[Dog](PersonSchema.dog) must beEqualTo(Some(Ref(DId(realMedorId))(medor.copy(id=Some(realMedorId)))))

          entity.get(PersonSchema.dogRef) must beEqualTo(Some(Ref(DId(realMedorId))(medor.copy(id=Some(realMedorId)))))

          val doggiesValue = entity.get(PersonSchema.doggies)
          doggiesValue must beEqualTo(Some(Set(
            Ref(DId(realDoggy1Id))(doggy1.copy(id=Some(realDoggy1Id))),
            Ref(DId(realDoggy2Id))(doggy2.copy(id=Some(realDoggy2Id))),
            Ref(DId(realDoggy3Id))(doggy3.copy(id=Some(realDoggy3Id)))
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
      
      val a = SchemaFact.retract(id)( PersonSchema.name -> "toto" )
      a must beEqualTo(RetractFact( id, person / "name", DString("toto") ))

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
          (PersonSchema.characters -> Set(violent, weak)) //+ 
          //(PersonSchema.specialChar -> clever) + 
          //(PersonSchema.dog -> medor) +
          //(PersonSchema.doggies -> Set(doggy1, doggy2, doggy3))

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
  }
  
}
