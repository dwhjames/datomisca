import org.specs2.mutable._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{Step, Fragments}

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
import scala.util.{Try, Success, Failure}
import java.util.concurrent.TimeUnit._

import datomisca._

import Datomic._
import DatomicMapping._

class DatomicTxSpec extends Specification {
  sequential

  import scala.concurrent.ExecutionContext.Implicits.global

  val uri = "datomic:mem://DatomicTxSpec"
  case class Person(name: String, age: Long)
  case class Dog(name: String, age: Long)
  case class PersonFriend(name: String, age: Long)
  case class PersonDog(name: String, age: Long, dog: Ref[Dog])
  case class PersonDogOpt(name: String, age: Long, dog: Option[Ref[Dog]])
  case class PersonDogList(name: String, age: Long, dogs: Set[Ref[Dog]])

  case class PersonLike(name: String, age: Long, like: Option[String] = None)
  case class PersonLikes(name: String, age: Long, likes: Set[String] = Set())

  object PersonSchema {
    val name = Attribute( KW(":person/name"), SchemaType.string, Cardinality.one).withDoc("Person's name")
    val age = Attribute( KW(":person/age"), SchemaType.long, Cardinality.one).withDoc("Person's age")
    val friend = Attribute( KW(":person/friend"), SchemaType.ref, Cardinality.one).withDoc("Person's friend")
    val dog = Attribute( KW(":person/dog"), SchemaType.ref, Cardinality.one).withDoc("Person's dog")
    val dogs = Attribute( KW(":person/dogs"), SchemaType.ref, Cardinality.many).withDoc("Person's dogs")
    val like = Attribute( KW(":person/like"), SchemaType.string, Cardinality.one).withDoc("Person's like")
    val likes = Attribute( KW(":person/likes"), SchemaType.string, Cardinality.many).withDoc("Person's likes")

    val schema = Seq(name, age, friend, dog, dogs, like, likes)
  }

  object DogSchema {
    val name = Attribute( KW(":dog/name"), SchemaType.string, Cardinality.one).withDoc("Dog's name")
    val age = Attribute( KW(":dog/age"), SchemaType.long, Cardinality.one).withDoc("Dog's age")

    val schema = Seq(name, age)
  }

  val person = new Namespace("person") {
    val character = Namespace("person.character")
  }

  val dog = Namespace("dog")      

  def startDB = {
    println("Creating DB with uri %s: %s".format(uri, Datomic.createDatabase(uri)))

    implicit val conn = Datomic.connect(uri)  
    
    Await.result(
      Datomic.transact(PersonSchema.schema ++ DogSchema.schema),
      Duration("2 seconds")
    )
  } 

  def stopDB = {
    deleteDatabase(uri)
    println("Deleted DB")
  }

  override def map(fs: => Fragments) = Step(startDB) ^ fs ^ Step(stopDB)

  "Datomic Entity Mappings" should {
    "1 - map simple entity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age" -> 30
        )
      ).flatMap{ tx => 
        println("Provisioned data... TX:%s".format(tx))

        println("Resolved Id for toto: temp(%s) real(%s)".format(idToto.toNative, tx.resolve(idToto)))

        val totoId = tx.resolve(idToto)
        Datomic.transact(
          Entity.add( DId(Partition.USER) )(
            person / "name" -> "tutu",
            person / "age" -> 54,
            person / "friend" -> totoId
          ),
          Entity.add( DId(Partition.USER) )(
            person / "name" -> "tata",
            person / "age" -> 23,
            person / "friend" -> totoId
          )
        ).map{ tx => 
          println("Provisioned more data... TX:%s".format(tx))

          Datomic.q(Query.manual[Args0, Args1]("""
            [ :find ?e 
              :where [ ?e :person/friend ?f ]
                     [ ?f :person/name "toto" ]
            ]              
          """)).map{
            case DLong(e) =>
              val entity = database.entity(e)
              val p @ Person(name, age) = DatomicMapping.fromEntity[Person](entity)
              println(s"Found person with name $name and age $age")
              p
          } must beEqualTo(List(Person("tutu", 54), Person("tata", 23)))  
        }
      }.recover{
        case e => failure(e.getMessage)
      }

      Await.result(
        fut,
        Duration("2 seconds")
      )

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
        person / "age" -> 30
      )

      val fut = Datomic.transact(
        toto
      ).flatMap{ tx => 
        println("2 Provisioned data... TX:%s".format(tx))

        println("2 Resolved Id for toto: temp(%s) real(%s)".format(idToto.toNative, tx.resolve(idToto)))
        val totoId = tx.resolve(toto)
        Datomic.transact(
          Entity.add(idTutu)(
            person / "name" -> "tutu",
            person / "age" -> 54,
            person / "friend" -> totoId
          ),
          Entity.add(idTata)(
            person / "name" -> "tata",
            person / "age" -> 23,
            person / "friend" -> totoId
          )
        ).map{ tx => 
          println("2 Provisioned more data... TX:%s".format(tx))

          Datomic.q(Query.manual[Args0, Args1]("""
            [ :find ?e 
              :where [ ?e :person/friend ?f ]
                     [ ?f :person/name "toto" ]
            ]
          """)).map{
            case DLong(e) =>
              val entity = database.entity(e)
              val Person(name, age) = DatomicMapping.fromEntity[Person](entity)
              println(s"2 Found person with name $name and age $age")
          }
          success
        }
      }.recover{
        case e => failure(e.getMessage)
      }

      Await.result(
        fut,
        Duration("2 seconds")
      )

    }

    "3 - convert a simple case class to AddEntity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personDogWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long]
      )(unlift(Person.unapply))

      val toto = Person("toto", 30)
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)
    }

    "4 - manage case class writing with references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age.read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age.write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long] and
        PersonSchema.dog.read[Ref[Dog]]
      )(PersonDog)

      implicit val personDogWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long] and
        PersonSchema.dog.write[Ref[Dog]]
      )(unlift(PersonDog.unapply))

      val medor = Dog("medor", 5)
      val medorId = DId(Partition.USER)
      val toto = PersonDog("toto", 30, Ref(medorId)(medor))
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30,
        person / "dog" -> medorId
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor)
      ).map{ tx =>
        println("2 Provisioned more data... TX:%s".format(tx))
        
        tx.resolve(medorId, totoId) match {
          case (medorId, totoId) => 
            println(s"4 totoId:$totoId medorId:$medorId")
            val entity = database.entity(totoId)
            val PersonDog(name, age, dog) = DatomicMapping.fromEntity[PersonDog](entity)
            println(s"Found Toto $name $age $dog")
          case _ => failure("unable to resolve ids")
        }
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }


    "5 - manage case class writing with optional field" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personLikeReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long] and
        PersonSchema.like.readOpt[String]
      )(PersonLike)

      implicit val personLikeWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long] and
        PersonSchema.like.writeOpt[String]
      )(unlift(PersonLike.unapply))

      val toto = PersonLike("toto", 30, Some("chocolate"))
      val totoId = DId(Partition.USER)

      val tutu = PersonLike("tutu", 45, None)
      val tutuId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30,
        person / "like" -> "chocolate"
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(tutuId)(tutu)
      ).map{ tx =>
        println("5 - Provisioned more data... TX:%s".format(tx))
        
        tx.resolve(totoId, tutuId) match {
          case (totoId, tutuId) => 
            println(s"5 - totoId:$totoId tutuId:$tutuId")
            val entityToto = database.entity(totoId)
            val t1 = DatomicMapping.fromEntity[PersonLike](entityToto)
            println(s"5 - retrieved toto:$t")
            t1.toString must beEqualTo(PersonLike("toto", 30, Some("chocolate")).toString)
            val entityTutu = database.entity(tutuId)
            val t2 = DatomicMapping.fromEntity[PersonLike](entityTutu)
            println(s"5 - retrieved tutu:$t")
            t2 must beEqualTo(tutu)
          case _ => failure("unable to resolve ids")
        }
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }


    "6 - manage case class writing with list field" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personLikesReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long] and
        PersonSchema.likes.read[Set[String]]
      )(PersonLikes)

      implicit val personLikesWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long] and
        PersonSchema.likes.write[Set[String]]
      )(unlift(PersonLikes.unapply))

      val toto = PersonLikes("toto", 30, Set("chocolate", "vanilla"))
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30,
        person / "likes" -> Set("chocolate", "vanilla")
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)
      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto)
      ).map{ tx =>
        println("5 - Provisioned more data... TX:%s".format(tx))
        
        val realTotoId = tx.resolve(totoId)
        println(s"6 - totoId:$totoId")
        val entity = database.entity(realTotoId)
        val t = DatomicMapping.fromEntity[PersonLikes](entity)
        println(s"5 - retrieved toto:$t")
        t must beEqualTo(PersonLikes("toto", 30, Set("chocolate", "vanilla")))
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }

    "7 - manage case class writing with optional references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age.read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age.write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogOptReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long] and
        PersonSchema.dog.readOpt[Ref[Dog]]
      )(PersonDogOpt)

      implicit val personDogOptWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long] and
        PersonSchema.dog.writeOpt[Ref[Dog]]
      )(unlift(PersonDogOpt.unapply))

      val medor = Dog("medor", 5)
      val medorId = DId(Partition.USER)
      val toto = PersonDogOpt("toto", 30, Some(Ref(medorId)(medor)))
      val totoId = DId(Partition.USER)

      val tutu = PersonDogOpt("tutu", 45, None)
      val tutuId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30,
        person / "dog" -> medorId
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor),
        DatomicMapping.toEntity(tutuId)(tutu)
      ).map{ tx =>
        println("7 - Provisioned more data... TX:%s".format(tx))
        
        val (realMedorId, realTotoId, realTutuId) = tx.resolve(medorId, totoId, tutuId)
        println(s"7 - totoId:$totoId medorId:$medorId")
        val entityToto = database.entity(realTotoId)
        val t1 = DatomicMapping.fromEntity[PersonDogOpt](entityToto)
        println(s"7 - retrieved toto:$t")
        t1.toString must beEqualTo(PersonDogOpt("toto", 30, Some(Ref(DId(realMedorId))(medor))).toString)

        val entityTutu = database.entity(realTutuId)
        val t2 = DatomicMapping.fromEntity[PersonDogOpt](entityTutu)
        println(s"7 - retrieved tutu:$t")
        t2 must beEqualTo(tutu)

      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }


    "8 - manage case class writing with list references" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val dogReader = (
        DogSchema.name.read[String] and 
        DogSchema.age.read[Long]
      )(Dog)

      implicit val dogWriter = (
        DogSchema.name.write[String] and 
        DogSchema.age.write[Long]
      )(unlift(Dog.unapply))

      implicit val personDogListReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long] and
        PersonSchema.dogs.read[Set[Ref[Dog]]]
      )(PersonDogList)

      implicit val personDogListWriter = (
        PersonSchema.name.write[String] and 
        PersonSchema.age.write[Long] and
        PersonSchema.dogs.write[Set[Ref[Dog]]]
      )(unlift(PersonDogList.unapply))

      val medor = Dog("medor", 5)
      val medorId = DId(Partition.USER)

      val brutus = Dog("brutus", 3)
      val brutusId = DId(Partition.USER)
      
      val toto = PersonDogList("toto", 30, Set(Ref(medorId)(medor), Ref(brutusId)(brutus)))
      val totoId = DId(Partition.USER)

      val totoEntity = Entity.add(totoId)(
        person / "name" -> "toto",
        person / "age" -> 30,
        person / "dogs" -> Set(medorId, brutusId)
      )

      DatomicMapping.toEntity(totoId)(toto).toString must beEqualTo(totoEntity.toString)
      println("8 - toto:"+toto+" TOTO ENTITY:"+DatomicMapping.toEntity(totoId)(toto))

      val fut = Datomic.transact(
        DatomicMapping.toEntity(totoId)(toto),
        DatomicMapping.toEntity(medorId)(medor),
        DatomicMapping.toEntity(brutusId)(brutus)
      ).map{ tx =>
        println("8 - Provisioned more data... TX:%s".format(tx))
        
        val (realMedorId, realBrutusId, realTotoId) = tx.resolve(medorId, brutusId, totoId)
        val entity = database.entity(realTotoId)
        val t = DatomicMapping.fromEntity[PersonDogList](entity)
        t must beEqualTo(PersonDogList("toto", 30, Set(Ref(DId(realMedorId))(medor), Ref(DId(realBrutusId))(brutus))))
      }      

      Await.result(
        fut,
        Duration("2 seconds")
      )
    }

    "9 - resolveEntity" in {
      implicit val conn = Datomic.connect(uri)  

      implicit val personReader = (
        PersonSchema.name.read[String] and 
        PersonSchema.age.read[Long]
      )(Person)

      val idToto = DId(Partition.USER)

      val fut = Datomic.transact(
        Entity.add(idToto)(
          person / "name" -> "toto",
          person / "age" -> 30
        )
      ).map{ tx => 
        val id = tx.resolve(idToto)
        database.entity(id) !== beNull
        
        database.entity(1234L) must throwA[datomisca.EntityNotFoundException]
        Datomic.resolveEntity(tx, DId(Partition.USER)) must throwA[datomisca.EntityNotFoundException]
      }
    }
  }

}