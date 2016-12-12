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

import DatomicMapping._

import scala.language.reflectiveCalls

import org.specs2.mutable._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class DatomicMapping2Spec extends Specification {
  sequential

  val uri = "datomic:mem://DatomicMapping2Spec"

  

  case class Person(id: Long, name: String, age: Long, birth: java.util.Date, characters: Set[Keyword], specialChar: Keyword, dog: Option[Dog] = None, doggies: Set[Dog])
  case class Dog(id: Option[Long], name: String, age: Long)

  case class Person2(
    id: Long, name: String, age: Long, birth: java.util.Date, 
    characters: Set[Keyword], specialChar: Keyword,
    dog: Option[Long] = None, doggies: Set[Long])

  case class Person3(
    id: Long, name: String, age: Long, birth: java.util.Date, 
    characters: Set[Keyword], specialChar: Keyword,
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
    val doggies     = Attribute(person / "doggies",     SchemaType.ref,     Cardinality.many).withDoc("Person's doggies")

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
    PersonSchema.characters .read[Set[Keyword]]   and
    PersonSchema.specialChar.read[Keyword]        and
    PersonSchema.dog        .readOpt[Dog]         and
    PersonSchema.doggies    .read[Set[Dog]]
  )(Person)

  implicit val person2Reader = (
    DatomicMapping.readId and
    PersonSchema.name       .read[String]         and 
    PersonSchema.age        .read[Long]           and
    PersonSchema.birth      .read[java.util.Date] and
    PersonSchema.characters .read[Set[Keyword]]   and
    PersonSchema.specialChar.read[Keyword]        and
    PersonSchema.dog        .readOpt[Long]        and
    PersonSchema.doggies    .read[Set[Long]]
  )(Person2)

  implicit val person3Reader = (
    DatomicMapping.readId and
    PersonSchema.name       .read[String]         and 
    PersonSchema.age        .read[Long]           and
    PersonSchema.birth      .read[java.util.Date] and
    PersonSchema.characters .read[Set[Keyword]]   and
    PersonSchema.specialChar.read[Keyword]        and
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
    Set(violent.ident, weak.ident), clever.ident, Some(medor), Set(doggy1, doggy2, doggy3)
  )
  val totobis = Person2(
    0L, "toto", 30L, birthDate, 
    Set(violent.ident, weak.ident), clever.ident, None, Set()
  )
  val totoId = DId(Partition.USER)
  var realTotoId: Long = _

  val toto2 = Person3(
    0L, "toto2", 30L, birthDate, 
    Set(violent.ident, weak.ident), clever.ident, None, None
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

            Datomic.q(Query("""
              [ :find ?e 
                :where [?e :person/name "toto"]
              ]
            """), Datomic.database).head match {
              case e: Long =>
                val entity = Datomic.database.entity(e)
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

      success

    }

    "read case class with ID" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query("""
        [ :find ?e 
          :where [?e :dog/name "medor"]
        ]
      """), Datomic.database).head match {
        case e: Long =>
          val entity = Datomic.database.entity(e)
          DatomicMapping.fromEntity[Dog](entity) must beEqualTo(medor.copy(id=Some(realMedorId)))
      }

      Datomic.q(Query("""
        [ :find ?e 
          :where [?e :person/name "toto"]
        ]
      """), Datomic.database).head match {
        case e: Long =>
          val entity = Datomic.database.entity(e)
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

      Datomic.q(Query("""
        [ :find ?e 
          :where [?e :person/name "toto2"]
        ]
      """), Datomic.database).head match {
        case e: Long =>
          val entity = Datomic.database.entity(e)
          DatomicMapping.fromEntity[Person3](entity) must beEqualTo(
            toto2.copy(
              id=realToto2Id
            ))
      }

      success
    }

    "get entity fields from attributes" in {

      implicit val conn = Datomic.connect(uri)

      Datomic.q(Query("""
        [ :find ?e 
          :where [?e :person/name "toto"]
        ]
      """), Datomic.database).head match {
        case e: Long =>
          val entity = Datomic.database.entity(e)

          entity(PersonSchema.name) must beEqualTo("toto")

          entity.get(PersonSchema.name) must beEqualTo(Some("toto"))

          entity.as[String](person / "name") must beEqualTo("toto")

          entity.as[Long](person / "age") must beEqualTo(30)

          val characters  = entity(PersonSchema.characters)
          val characters2 = entity.getAs[Set[Keyword]](person / "characters")

          entity.as[java.util.Date](person / "birth") must beEqualTo(birthDate)

          entity.get(PersonSchema.birth) must beEqualTo(Some(birthDate))

          val dogValue0 = entity.getAs[Entity](person / "dog")

          entity.getIdView[Dog](PersonSchema.dog) must beEqualTo(Some(IdView(realMedorId)(medor.copy(id=Some(realMedorId)))))

          val doggiesValue = entity.getIdViews[Dog](PersonSchema.doggies)
          doggiesValue must beEqualTo(Some(Set(
            IdView(realDoggy1Id)(doggy1.copy(id=Some(realDoggy1Id))),
            IdView(realDoggy2Id)(doggy2.copy(id=Some(realDoggy2Id))),
            IdView(realDoggy3Id)(doggy3.copy(id=Some(realDoggy3Id)))
          )))

          val writer = PersonSchema.specialChar.write[AddIdent]
          writer.write(clever).toMap must beEqualTo(
            Map(
              PersonSchema.specialChar.ident -> clever.ident
            )
          )
      }

      success
    }

  }
  
}
