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

import org.specs2.mutable._

import org.specs2.specification.core.Fragments

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class DatomicTxSpec extends Specification {
  sequential


  val uri = "datomic:mem://DatomicTxSpec"

  case class Person(
    name: String,
    age:  Long
  )
  case class Dog(
    name: String,
    age:  Long
  )

  case class PersonFriend(
    name: String,
    age:  Long
  )

  case class PersonDog(
    name: String,
    age:  Long,
    dog:  IdView[Dog]
  )
  case class PersonDogOpt(
    name: String,
    age:  Long,
    dog:  Option[IdView[Dog]]
  )
  case class PersonDogList(
    name: String,
    age:  Long,
    dogs: Set[IdView[Dog]]
  )

  case class PersonLike(
    name: String,
    age:  Long,
    like: Option[String] = None
  )
  case class PersonLikes(
    name:  String,
    age:   Long,
    likes: Set[String] = Set()
  )

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }

  val dog = Namespace("dog")  

  object PersonSchema {
    val name   = Attribute(person / "name",   SchemaType.string, Cardinality.one) .withDoc("Person's name")
    val age    = Attribute(person / "age",    SchemaType.long,   Cardinality.one) .withDoc("Person's age")
    val friend = Attribute(person / "friend", SchemaType.ref,    Cardinality.one) .withDoc("Person's friend")
    val dog    = Attribute(person / "dog",    SchemaType.ref,    Cardinality.one) .withDoc("Person's dog")
    val dogs   = Attribute(person / "dogs",   SchemaType.ref,    Cardinality.many).withDoc("Person's dogs")
    val like   = Attribute(person / "like",   SchemaType.string, Cardinality.one) .withDoc("Person's like")
    val likes  = Attribute(person / "likes",  SchemaType.string, Cardinality.many).withDoc("Person's likes")

    val schema = Seq(name, age, friend, dog, dogs, like, likes)
  }

  object DogSchema {
    val name = Attribute(dog / "name", SchemaType.string, Cardinality.one).withDoc("Dog's name")
    val age  = Attribute(dog / "age",  SchemaType.long,   Cardinality.one).withDoc("Dog's age")

    val schema = Seq(name, age)
  }

  def startDB(): Unit = {
    println(s"Creating DB with uri $uri: ${Datomic.createDatabase(uri)}")

    implicit val conn = Datomic.connect(uri)  
    
    Await.result(
      Datomic.transact(PersonSchema.schema ++ DogSchema.schema),
      Duration("2 seconds")
    )
    ()
  } 

  def stopDB(): Unit = {
    Datomic.deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = step(startDB) ^ fs ^ step(stopDB)

  "Datomic Entity Mappings" should {
    "1 - map simple entity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age" -> 30
        )
      ) flatMap { tx => 
        println(s"Provisioned data... TX: $tx")

        println(s"Resolved Id for toto: temp(${idToto}) real(${tx.resolve(idToto)})")

        val totoId = tx.resolve(idToto)
        Datomic.transact(
          Entity.add( DId(Partition.USER) )(
            person / "name"   -> "tutu",
            person / "age"    -> 54,
            person / "friend" -> totoId
          ),
          Entity.add( DId(Partition.USER) )(
            person / "name"   -> "tata",
            person / "age"    -> 23,
            person / "friend" -> totoId
          )
        ) map { tx => 
          println(s"Provisioned more data... TX: $tx")

          Datomic.q(Query("""
            [ :find ?e 
              :where [ ?e :person/friend ?f ]
                     [ ?f :person/name "toto" ]
            ]              
          """), Datomic.database) map {
            case e: Long =>
              val entity = Datomic.database.entity(e)
              val p @ Person(name, age) = DatomicMapping.fromEntity[Person](entity)
              println(s"Found person with name $name and age $age")
              p
          } must beEqualTo(List(Person("tutu", 54), Person("tata", 23)))  
        }
      }

      Await.result(
        fut,
        Duration("2 seconds")
      )

      success
    }

    "2 - resolve id of an inserted entity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long]
      )(Person)

      val idToto = DId(Partition.USER)
      val idTutu = DId(Partition.USER)
      val idTata = DId(Partition.USER)

      val toto = Entity.add(idToto)(
        person / "name" -> "toto",
        person / "age"  -> 30
      )

      val fut = Datomic.transact(
        toto
      ) flatMap { tx => 
        println(s"2 Provisioned data... TX: $tx")

        println(s"2 Resolved Id for toto: temp(${idToto}) real(${tx.resolve(idToto)})")
        val totoId = tx.resolve(toto)
        Datomic.transact(
          Entity.add(idTutu)(
            person / "name"   -> "tutu",
            person / "age"    -> 54,
            person / "friend" -> totoId
          ),
          Entity.add(idTata)(
            person / "name"   -> "tata",
            person / "age"    -> 23,
            person / "friend" -> totoId
          )
        ) map { tx => 
          println(s"2 Provisioned more data... TX: $tx")

          Datomic.q(Query("""
            [ :find ?e 
              :where [ ?e :person/friend ?f ]
                     [ ?f :person/name "toto" ]
            ]
          """), Datomic.database) map {
            case e: Long =>
              val entity = Datomic.database.entity(e)
              val Person(name, age) = DatomicMapping.fromEntity[Person](entity)
              println(s"2 Found person with name $name and age $age")
          }
          success
        }
      }

      Await.result(
        fut,
        Duration("2 seconds")
      )

      success
    }

    "3 - convert a simple case class to AddEntity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personDogWriter = (
        PersonSchema.name.write[String] and
        PersonSchema.age .write[Long]
      )(unlift(Person.unapply))

      val toto = Person("toto", 30)
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age"  -> 30
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      success
    }

    /*
    "4 - manage case class writing with references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age .read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age .write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]   and
        PersonSchema.dog .read[IdView[Dog]]
      )(PersonDog)

      implicit val personDogWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age .write[Long]   and
        PersonSchema.dog .write[IdView[Dog]]
      )(unlift(PersonDog.unapply))

      val medor   = Dog("medor", 5)
      val medorId = DId(Partition.USER)

      val toto    = PersonDog("toto", 30, IdView(medorId)(medor))
      val totoId  = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age"  -> 30,
        person / "dog"  -> medorId
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor)
      ) map { tx =>
        println(s"2 Provisioned more data... TX: $tx")
        val txIds = tx.tempidMap
        println(s"4 totoId:${txIds(totoId)} medorId:${txIds(medorId)}")
        val entity = Datomic.database.entity(txIds(totoId))
        val PersonDog(name, age, dog) = DatomicMapping.fromEntity[PersonDog](entity)
        println(s"Found Toto $name $age $dog")
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }
    */


    "5 - manage case class writing with optional field" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personLikeReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]   and
        PersonSchema.like.readOpt[String]
      )(PersonLike)

      implicit val personLikeWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long]    and
        PersonSchema.like.writeOpt[String]
      )(unlift(PersonLike.unapply))

      val toto   = PersonLike("toto", 30, Some("chocolate"))
      val totoId = DId(Partition.USER)

      val tutu   = PersonLike("tutu", 45, None)
      val tutuId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age"  -> 30,
        person / "like" -> "chocolate"
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(tutuId)(tutu)
      ) map { tx =>
        println(s"5 - Provisioned more data... TX: $tx")
        val txIds = tx.tempidMap
        println(s"5 - totoId:${txIds(totoId)} tutuId:${txIds(tutuId)}")
        val entityToto = Datomic.database.entity(txIds(totoId))
        val t1 = DatomicMapping.fromEntity[PersonLike](entityToto)
        println(s"5 - retrieved toto:$t")
        t1.toString must beEqualTo(PersonLike("toto", 30, Some("chocolate")).toString)
        val entityTutu = Datomic.database.entity(txIds(tutuId))
        val t2 = DatomicMapping.fromEntity[PersonLike](entityTutu)
        println(s"5 - retrieved tutu:$t")
        t2 must beEqualTo(tutu)
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )

      success
    }


    "6 - manage case class writing with list field" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personLikesReader = (
        PersonSchema.name .read[String] and 
        PersonSchema.age  .read[Long]   and
        PersonSchema.likes.read[Set[String]]
      )(PersonLikes)

      implicit val personLikesWriter = (
        PersonSchema.name .write[String] and 
        PersonSchema.age  .write[Long]   and
        PersonSchema.likes.write[Set[String]]
      )(unlift(PersonLikes.unapply))

      val toto   = PersonLikes("toto", 30, Set("chocolate", "vanilla"))
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name"  -> "toto",
        person / "age"   -> 30,
        person / "likes" -> Set("chocolate", "vanilla")
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)
      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto)
      ) map { tx =>
        println(s"5 - Provisioned more data... TX: $tx")
        
        val realTotoId = tx.resolve(totoId)
        println(s"6 - totoId:$totoId")
        val entity = Datomic.database.entity(realTotoId)
        val t = DatomicMapping.fromEntity[PersonLikes](entity)
        println(s"5 - retrieved toto:$t")
        t must beEqualTo(PersonLikes("toto", 30, Set("chocolate", "vanilla")))
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )

      success
    }

    /*
    "7 - manage case class writing with optional references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age .read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age .write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogOptReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]   and
        PersonSchema.dog .readOpt[IdView[Dog]]
      )(PersonDogOpt)

      implicit val personDogOptWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age .write[Long]   and
        PersonSchema.dog .writeOpt[IdView[Dog]]
      )(unlift(PersonDogOpt.unapply))

      val medor   = Dog("medor", 5)
      val medorId = DId(Partition.USER)

      val toto    = PersonDogOpt("toto", 30, Some(IdView(medorId)(medor)))
      val totoId  = DId(Partition.USER)

      val tutu   = PersonDogOpt("tutu", 45, None)
      val tutuId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age"  -> 30,
        person / "dog"  -> medorId
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor),
        DatomicMapping.toEntity(tutuId)(tutu)
      ) map { tx =>
        println(s"7 - Provisioned more data... TX: $tx")
        
        val Seq(realMedorId, realTotoId, realTutuId) = Seq(medorId, totoId, tutuId) map tx.resolve
        println(s"7 - totoId:$totoId medorId:$medorId")
        val entityToto = Datomic.database.entity(realTotoId)
        val t1 = DatomicMapping.fromEntity[PersonDogOpt](entityToto)
        println(s"7 - retrieved toto:$t")
        t1.toString must beEqualTo(PersonDogOpt("toto", 30, Some(IdView(DId(realMedorId))(medor))).toString)

        val entityTutu = Datomic.database.entity(realTutuId)
        val t2 = DatomicMapping.fromEntity[PersonDogOpt](entityTutu)
        println(s"7 - retrieved tutu:$t")
        t2 must beEqualTo(tutu)

      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }
    */

    /*
    "8 - manage case class writing with list references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age .read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age .write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogListReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]   and  
        PersonSchema.dogs.read[Set[IdView[Dog]]]
      )(PersonDogList)

      implicit val personDogListWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age .write[Long]   and
        PersonSchema.dogs.write[Set[IdView[Dog]]]
      )(unlift(PersonDogList.unapply))

      val medor   = Dog("medor", 5)
      val medorId = DId(Partition.USER)

      val brutus   = Dog("brutus", 3)
      val brutusId = DId(Partition.USER)
      
      val toto   = PersonDogList("toto", 30, Set(IdView(medorId)(medor), IdView(brutusId)(brutus)))
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age"  -> 30,
        person / "dogs" -> Set(medorId, brutusId)
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)
      println(s"8 - toto: ${toto} TOTO ENTITY: ${DatomicMapping.toEntity(totoId)(toto)}")

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor),
        DatomicMapping.toEntity(brutusId)(brutus)
      ) map { tx =>
        println(s"8 - Provisioned more data... TX: $tx")
        
        val Seq(realMedorId, realBrutusId, realTotoId) = Seq(medorId, brutusId, totoId) map tx.resolve
        val entity = Datomic.database.entity(realTotoId)
        val t = DatomicMapping.fromEntity[PersonDogList](entity)
        t must beEqualTo(PersonDogList("toto", 30, Set(IdView(DId(realMedorId))(medor), IdView(DId(realBrutusId))(brutus))))
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }
    */

    "9 - resolveEntity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age"  -> 30
        )
      ) map { tx => 
        val id = tx.resolve(idToto)
        Datomic.database.entity(id) !== beNull
        
        Datomic.database.entity(1234L).keySet must beEmpty
        tx.resolveEntity(DId(Partition.USER)).keySet must beEmpty
      }

      success
    }

    "10 - get txReport map/toString" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age"  -> 30
        )
      ) map { tx => 
        val id = tx.resolve(idToto)
        
        println(tx.toString)
      }

      success
    }

     "11 - get txReport extract" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age .read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age"  -> 30
        )
      ) map { tx => 
         val entries = tx.txData.collect{ case Datom(_,k,v,_,_) => (k.toLong, v)}.toMap
         val db = tx.dbAfter
         entries.size must beEqualTo(3)
         entries(db.entid(person / "age")) must beEqualTo(30)
         entries(db.entid(person / "name"))  must beEqualTo("toto")
         entries(db.entid(PersonSchema.name.ident))  must beEqualTo("toto")
         
         tx.txData.collectFirst{ case Datom(_,_, age: Long,_,_) => age} must beEqualTo(Some(30))
         tx.txData.collectFirst{ case Datom(_,k , name: String ,_,_) if db.ident(k) == PersonSchema.name.ident => name} must beEqualTo(Some("toto"))
      }

      Await.result(fut,Duration("2 seconds"))
    }

    "12 - upsert an entity using it's lookup ref as id" in {
      import ColourPreference.Implicits._
      implicit val connection = Datomic.connect(uri)
      transactSync(ColourPreference.schema:_*)
      val entityId = GIVEN_anEntity(ColourPreference.Entity("bob@rainbow.org", "grey"))

      transactSync(toEntity(LookupRef(ColourPreference.email, "bob@rainbow.org"))(ColourPreference.Entity("robert@rainbow.com", "taupe")))

      fromEntity(connection.database.entity(entityId)) must be_==(ColourPreference.Entity("robert@rainbow.com", "taupe"))
    }

    "13 - fetch an entity using it's lookup ref as id" in {
      import ColourPreference.Implicits._
      implicit val connection = Datomic.connect(uri)
      transactSync(ColourPreference.schema:_*)
      GIVEN_anEntity(ColourPreference.Entity("bob@rainbow.org", "grey"))

      val fetchedEntity = fromEntity(connection.database.entity(LookupRef(ColourPreference.email, "bob@rainbow.org")))

      fetchedEntity must be_==(ColourPreference.Entity("bob@rainbow.org", "grey"))
    }

    "14 - retract a fact about an entity using it's lookup ref as id" in {
      import ColourPreference.Implicits._
      implicit val connection = Datomic.connect(uri)
      transactSync(ColourPreference.schema:_*)
      val entityId = GIVEN_anEntity(ColourPreference.Entity("bob@rainbow.org", "grey"))

      val lookupRef: LookupRef = LookupRef(ColourPreference.email, "bob@rainbow.org")
      transactSync(SchemaFact.retract(lookupRef)(ColourPreference.favouriteColour -> "grey"))

      connection.database.entity(entityId).get(ColourPreference.favouriteColour) must beNone
    }
  }

  private def GIVEN_anEntity[T](entity: T)(implicit connection: Connection, writer: PartialAddEntityWriter[T]) = {
    val tempId = DId(Partition.USER)
    val report = Await.result(Datomic.transact(toEntity(tempId)(entity)), Duration("1 second"))
    report.resolve(tempId)
  }

  private def transactSync(txData: TxData*)(implicit connection: Connection) = {
    Await.result(Datomic.transact(txData), Duration("1 second"))
  }


  private object ColourPreference {
    val org = Namespace("org")
    val email = Attribute(org / "email", SchemaType.string, Cardinality.one).withUnique(Unique.identity)
    val favouriteColour = Attribute(org / "favouriteColour", SchemaType.string, Cardinality.one).withUnique(Unique.identity)
    val schema = Seq(email, favouriteColour)
    case class Entity(email: String, favouriteColour: String)

    object Implicits {
      implicit val Writer: PartialAddEntityWriter[Entity] =
        (email.write[String] ~ favouriteColour.write[String]).apply {
          (e: Entity) => (e.email, e.favouriteColour)
        }
      implicit val Reader: EntityReader[Entity] =
        (email.read[String] ~ favouriteColour.read[String])(Entity)
    }
  }

}
