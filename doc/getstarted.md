---
layout: default
title: Getting Started
---

# Getting Started

Here is a very simple sample to begin with Datomisca.

## Requirements

- SBT v0.12.x

## Github project

You can find this sample in Datomic Github Samples [Getting-Started](https://github.com/pellucidanalytics/datomisca/tree/master/samples/getting-started)


## #1 Add resolvers to SBT

You can add that in your `build.sbt` or `Build.scala depending on your choice.

```scala
resolvers ++= Seq(
  // to get Datomisca
  "Pellucid Bintray"  at "http://dl.bintray.com/content/pellucid/maven",
  // to get Datomic free (for pro, you must put in your own repo or local)
  "clojars" at "https://clojars.org/repo"
)
```

## #2 Add dependencies

> Last stable version is 0.5.1

```scala
libraryDependencies ++= Seq(
  "com.pellucid" %% "datomisca" % "0.5.1",
  "com.datomic" % "datomic-free" % "0.8.4020.26"
)
```

## #3 Default import

You could be more precise but following imports ensure you have everything required in your scope including a few required implicits.

```scala
import datomisca._
```


## #4 Create Datomic implicit connection

To use Datomisca facilities, you need an implicit connection to Datomic in your scope.

```scala
// Datomic URI definition
val uri = "datomic:mem://datomisca-getting-started"

// Datomic Connection as an implicit in scope
implicit lazy val conn = Datomic.connect(uri)
```

_You can also use explicit connection in case you have several but generally this is not the case._

Please note that having an implicit connection in your scope also provides you with an implicit Datomisca `DDatabase which gives access to an Scala-enhanced Datomic Database object.

```scala
implicit def database(implicit conn: Connection): DDatabase
```

## #5 Create Datomic DB

We start from scratch so let's first create a DB.

```scala
// This returns a boolean telling if worked or not
Datomic.createDatabase(uri)
```

## #6 Create Schema in Datomisca way

Datomisca allows to define your Schema in a programmatic way.

Here you create your:

- namespaces, 
- attributes
- enumerated entities

Attributes and enumerated entites are gathered in a schema representing your entity.

Let's create our first entity, a `Person` having four attributes:

- `name:       SchemaType.string,  Cardinality.one`
- `age:        SchemaType.long,    Cardinality.one`
- `birth:      SchemaType.instant, Cardinality.one`
- `characters: SchemaType.ref,     Cardinality.many`

```scala
object PersonSchema {
  // Namespaces definition to be reused in Schema
  object ns {
    val person = new Namespace("person") {
      val character = Namespace("person.character")
    }
  }

  // Attributes
  val name = Attribute(
    ns.person / "name",
    SchemaType.string,
    Cardinality.one).withDoc("Person's name")
  val age = Attribute(
    ns.person / "age",
    SchemaType.long,
    Cardinality.one).withDoc("Person's name")
  val birth = Attribute(
    ns.person / "birth",
    SchemaType.instant,
    Cardinality.one).withDoc("Person's birth date")
  val characters = Attribute(
    ns.person / "characters",
    SchemaType.ref,
    Cardinality.many).withDoc("Person's characters")

  // Characters enumerated values
  val violent = AddIdent(ns.person.character / "violent")
  val weak    = AddIdent(ns.person.character / "weak")
  val clever  = AddIdent(ns.person.character / "clever")
  val dumb    = AddIdent(ns.person.character / "dumb")
  val stupid  = AddIdent(ns.person.character / "stupid")

  // Schema
  val txData = Seq(
    name, age, birth, characters, // attributes
    violent, weak, clever, dumb, stupid // idents
  )

}

```

Please note that :

 - `Namespace is just a helper making code clearer when creating keywords
 - `Attribute` and `AddIdent` are just helpers creating Datomic operations.
 - Schema is just a `Seq[Operation]`
 - Person is just a way to gather Schema information but it has no real existence outside this.

## #7 Provision your schema into Datomic

Hey, we have a schema, now let's insert it into Datomic.
This is our 1st operation using Datomic transactor and as you may know, Datomisca manages transactor's communication in an asynchronous and non-blocking way based on Scala 2.10 Execution Context.

To ask the transaction to perform some operations, we use the following method:

```scala
Datomic.transact(ops: Seq[Operation]): Future[TxReport]
```

As as you can see, it just accepts operations and returns a `Future[TxReport]`.

>This is where the Future monadic composition begins.
>For more information, go to Internet, there are lots of information about the way you should work with Scala Futures

So let's insert our schema into Datomic:

```scala
Datomic.transact(PersonSchema.txData) flatMap { tx =>
  ...
  // do something
  ...
}
```

> We use `flatMap` because we expect to perform other transactions returning other Future so we will compose them.

## #8 Create your 1st entity

All atomic fact operations creation helpers are available in 
```scala
Fact.add/retract/partition
```

All entity operations creation helpers are available in 
```scala
Entity.add/retract
```

>Please note that Datomisca wraps all Datomic/Clojure types in Datomisca specific types to provide a real abstraction layer. So we have the following Datomisca types:
>
>- `DString`
>- `DLong`
>- `DFloat`
>- `DDouble`
>- `DBigDecimal`
>- `DBigInt`
>- `DInstant`
>
>And more special Datomisca types:
>
>- `DId` which represents a Datomic Id as:
>   - `TempId` : a temporary ID used to identify entities/facts before insertion
>   - `FinalId` : a final ID generated by Datomic for insertion
>- `DEntity` representing Datomic Entities retrieve using `database.entity(DId)`
>- `DDatabase` representing Datomic Database which can be passed to a Query too

Now let's create your first Person entity.

```scala
// John temporary ID
val johnId = DId(Partition.USER)
// John person entity  
val john = Entity.add(johnId)(
  Person.person / "name"       -> "John",
  Person.person / "age"        -> 35L,
  Person.person / "birth"      -> new java.util.Date(),
  Person.person / "characters" -> Set( Person.violent, Person.clever )
)
```

Please note:

- `DId(Partition.USER)` generates a new temporary ID which could be stored in a val if you wanted to use to reference this entity
- `Person.person / "name"       -> "John"` is in fact `Keyword -> DatomicData` so it should be `DString("John")` but Datomisca provides a clean implicit conversion for that.
- `Entity.add` is just a helper to create an operation adding several facts associated with the same DId

## #9 Provision your entity into Datomic

Adding an entity is just like adding the schema as both are operations. 

```scala
// creates an entity
Datomic.transact(john) map { tx =>
  val realJohnId = tx.resolve(johnId)
  ...
  // Do something else
}
```

Please note:

- `tx.resolve(johnId)` is used to retrieve the real Id after insertion from temporary Id.
- you can also retrieve several ID at the same time:

```scala
val Seq(realId1, realId2, realId3) = tx.resolve(id1, id2, id3)
```

## #10 Write your first query

So now that we have an entity in our DB, let's try to query it.

In Datomisca, you **write your queries in Datalog exactly in the same way as Clojure**. What Datomisca provides on top of that is that, based on Scala Macro 2.10, it **validates the syntax of you query at compile-time and also deduce the number of input/output parameters** (more features are also in the roadmap).

Let's write a "find person by name" query:

```scala
val queryFindByName = Query("""
  [ :find ?e ?age
    :in $ ?name
    :where [?e :person/name ?name]
           [?e :person/age ?age]
  ]
""")
```

Please note:

- This creates a query awaiting 2 input parameters ($ and ?age) and returning 2 ouput parameters (?e ?age)
- the query is a static structure and you can declare it once and reuse it as much as you want.
- `$` identifies the database as an input data source
- `?name` is our "by name" input parameter

## #11 Execute your first query

You just execute by calling `Datomic.q` on your query with the right input parameters.

The input parameters must all be expressed using one of the previously describeds Datomic types for now (_maybe we will provide pure Scala typing later_).

```scala
val results = Datomic.q(queryFindByName, Datomic.database, DString("John"))
```

Please note:

- `Datomic.q` expects a query and the right number of input parameters according to your query (here 2)
- `Datomic.database` is the ‘current’ database value available from the connection, and we force you to give it explicitly because in Datomic this is also mandatory and we want to respect Datomic choices.

## #12 Use query result

After executing previous query, you retrieve `results`.  
According to the input query, the compiler has infered that there should be 2 output parameters. 

Thus, `results` is a `Iterable[(DatomicData, DatomicData)]`.

```scala
results.headOption map { 
  case (DLong(e), DLong(age)) =>
    ...
    // do something
}
```

Please note:

- Note that results is a `Iterable[(DatomicData, DatomicData)]` and not `Iterable[(DLong, DLong)]` as you would expect. Why? Because with the info provided in the query, it's impossible to infer those types directly. In the roadmap, we foresee to provide type-safety for output parameters using Datomic Schema. And maybe we could almost deduce it should be a `Iterable[(Long, Long)]`... Be patient ;)
- if you don't give the right number of output parameters in the `case`, you should have a compiling error such as:

## #13 Use query result

With the previous query, we are retrieved `e` which is an entity ID and now we could get the entity from Datomic directly and then inspect entity fields.

```scala
val entity: DEntity = Datomic.database.entity(e)

val (johnName, johnAge, johnBirth, johnCharacters) = (
  entity.as[String]        (Person.person / "name"),
  entity.as[Long]          (Person.person / "age"),
  entity.as[java.util.Date](Person.person / "birth"),
  entity.as[Set[DKeyword]] (Person.person / "characters")
)
```

Please note:

- `DEntity` provides lots of helpers to access entity fields and convert them into Scala types. For more info, go in [DEntity ScalaDoc]({{ site.baseurl }}/api/current/#datomisca.DEntity)


## #14 Awaiting result

In this sample, we run the code in a pure ScalaApp.  
So as transactions are async/non-blocking, we must wait for results at the end of execution to be sure everything was OK.

```scala
Await.result(res, Duration("2 seconds"))
```

## #15 Using Datomisca in SBT environment

When using Future in Scala, you need to provide an ExecutionContext representing the pool of threads on which your async request is going to be executed. This allows to provide non-blocking behavior.  
But there is a weird behavior when running Datomisca in SBT console: the 1st time it runs perfectly, the 2nd time it fails with horrible Datomic exceptions "class not found in cache". This is linked to the way SBT manages classloaders and ExecutionContex apparently. 

_Please note this problem only appears when running in SBT and not when running in pure Scala_

To go around this problem, we have found that we had to explicitly shut down the Execution Context before end of execution. But this is not possible with default Scala global Execution Context because `shutdown` is a method of `ExecutionService` and we can't access to this service from Scala global.  

Hopefully, Datomisca provides its own Execution Context helper and provides an `ExecutionService`. This Execution Context is a copy of Scala one exposing its service so don't care if you use instead Scala one.

```scala
  // import Datomisca execution context & service
  import datomisca.executioncontext.ExecutionContextHelper._
  ...
  defaultExecutorService.shutdownNow()
```


## #16 And much more

You can also:

- Create type-safe mappers from Scala structures/case-classes from/to entity
- Create type-safe entities using schema attributes
- Use scala variables in queries
- Create query Datalog rules and even use query functions
- Use all Datomic database manipulations (with, asOf...)

This already works but is not yet documented. Go in unit tests or [play-datomisca](http://github.com/pellucidanalytics/play-datomisca) samples too which uses those kind of features.

The only big feature missing for now is `database functions` which are functions declared in Java or Clojure and executed on transactor side. This will come later...

