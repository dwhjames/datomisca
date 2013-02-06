---
layout: default
title: Features
---

# <a name="features">Raw API Features</a>

## <a name="features-reactive">Reactive Transactor API (Asynchronous &amp; Non-Blocking with potential execution isolation)</a>
Using Scala 2.10 Execution Contexts &amp; Futures, Datomic transactions are executed by Datomisca in an asynchronous & non-blocking way managed by the provided execution context. In this way, you can control in which pool of threads you want to execute your remote transactor requests.

```scala
val person = Namespace("person")

Datomic.transact(
  Seq(operation1, operation2, operation3, ...)
) map { tx =>
  ...
}
```

<br/>
## <a name="features-scalatypes">Datomic/Clojure to Scala types conversion</a>

When you create or access Datomic entities, you retrieved Datomic types (ie Clojure types) and from Java API, all those types are seen as `Object` which is not really useful. So you could end into using `.asInstanceOf[T]`everywhere. Hopefully, Datomisca provides some conversion from/to Datomic types.


```scala
val s: DString = Datomic.toDatomic("toto")
val l: DLong = Datomic.toDatomic("5L")

val l: String = Datomic.fromDatomic(DString("toto"))
val s: Long = Datomic.fromDatomic(DLong(5L))

val entity = database.entity(entityId)
val name = entity.as[String](person / "name")
val age = entity.as[Long](person / "age")
```

<br/>
## <a name="features-compilequeries">Compile-Time query validation &amp; input/output parameters inference</a>

Based on Scala 2.10 Macros, Datomisca provides :

- **Compile-time validation of Datomic query strings** and then detect where there are errors.
- **Compile-time inference of input/output parameters** (number for now) so when you execute the query  , you must pass the right number of input parameters and manage the right number of output parameters.

```scala    
// Valid query
// produces a Query with :
//   - 2 input arguments (db and ?char)
//   - 2 output arguments (?e ?n)
scala> Query("""
     |       [ :find ?e ?n 
     |         :in $ ?char
     |         :where  [ ?e :person/name ?n ] 
     |                 [ ?e person/character ?char ]
     |       ]
     |     """)
res0: TypedQueryAuto2[DatomicData,DatomicData,(DatomicData, DatomicData)] = [ :find ?e ?n :in $ ?char :where [?e :person/name ?n] [?e :person/character ?char] ]


// Invalid query with missing ":" 
// error at compile-time
scala> Query("""
     |  [ :find ?e ?n 
     |    :in $ ?char
     |    :where  [ ?e :person/name ?n ] 
     |            [ ?e person/character ?char ]
     |  ]
     | """)
<console>:15: error: `]' expected but `p' found
                [ ?e person/character ?char ]
                     ^
```

_In the future, based on type-safe Schema presented below, we will also be able to infer parameter types._


<br/>
## <a name="features-staticqueries">Query as static reusable structures</a>

This is a very important idea in Datomic: **a query is a static structure** which can be built once and reused as many times as you want.

```scala
val query = Query("""
  [ :find ?e ?n 
    :in $ ?char
    :where  [ ?e :person/name ?n ] 
            [ ?e :person/character ?char ]
  ]
""")
      
Datomic.q( query, database, DRef(person.character/violent) ) map {
  case (e: DLong, n: DString) => // do something
}

Datomic.q( query, database, DRef(person.character/clever) ) map {
  case (e: DLong, n: DString) => // do something
}
```
<br/>
## <a name="features-ops">Programmatic Datomic operations</a>

You can build your operations `add` / `retract` / `addEntity` / `retractEntity` operations in a type-safe way.

```scala
val person = Namespace("person")

Datomic.transact(
  // Atomic Fact ops
  Fact.add(DId(Partition.USER))(person / "name" -> "tata")
  Fact.retract(DId(Partition.USER))(person / "name" -> "titi")
  Fact.partition(Partition(Namespace.DB.PART / "mypart"))

  // Entity ops
  Entity.add(DId(Partition.USER))(
    person / "name" -> "toto",
    person / "age" -> 30L
  ),
  Entity.retract(entityId)
) map { tx =>
  ...
}
```
<br/>
## <a name="features-schema">Static-typed &amp; programmatic Schema definition API</a>

Schema is one of the remarkable specific features of Datomic as it enables contraints on the type and cardinality of the inserted data.  
Schema attributes are just facts stored in Datomic in a special partition defining the parameters of an attribute : 
As Scala is static-typed language, it seems really logical to link those attributes to 

```scala
val uri = "datomic:mem://datomicschemaqueryspec"

val person = new Namespace("person") {
  val character = Namespace("person.character")
}

val violent = AddIdent(person.character / "violent")
val weak = AddIdent(Keyword(person.character, "weak"))
val clever = AddIdent(Keyword(person.character, "clever"))
val dumb = AddIdent(Keyword(person.character, "dumb"))

val name = Attribute( 
  person / "name", 
  SchemaType.string, 
  Cardinality.one).withDoc("Person's name")

val age = Attribute( 
  person / "age", 
  SchemaType.long, 
  Cardinality.one).withDoc("Person's age")

val characters = Attribute( 
  person / "character"), 
  SchemaType.ref, 
  Cardinality.many).withDoc("Person's characters")

val schema = Seq(
  // attributes
  name, age, characters,
  // enumerated values
  violent, weak, clever, dumb
)

Datomic.transact(schema) map { tx =>
  ...
}
```

<br/>
## <a name="features-dtm-parsing">Datomic DTM files runtime parsing</a>

If you wrote your schema in a DTM file for example, you can load and parse it at runtime.

```scala
// example with Datomic seattle sample schema
val schemaIs = current.resourceAsStream("seattle-schema.dtm").get
val schemaContent = Source.fromInputStream(schemaIs).mkString
val schema = Datomic.parseOps(schemaContent)

Datomic.transact(schema) map { tx =>
  ...
}
```


<br/>
# <a name="features">Extended Features</a>

## <a name="features-typesafe-ops">Type-safe Datomic operations using Schema</a>

Based on previously described static-typed schema, you can build your operations `add` / `retract` / `addEntity` / `retractEntity` operations in a type-safe way.

```scala
val person = Namespace("person")

object PersonSchema {
  val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's name")
  val birth = Attribute( person / "birth", SchemaType.instant, Cardinality.one).withDoc("Person's birth date")
}

// OK
SchemaFact.add(DId(Partition.USER))( PersonSchema.name -> "toto" ) 
// ERROR at compile-time
SchemaFact.add(DId(Partition.USER))( PersonSchema.name -> 123L )   

// OK
val e = SchemaEntity.add(DId(Partition.USER))( Props() +
  (PersonSchema.name -> "toto") +
  (PersonSchema.age -> 45L) +
  (PersonSchema.birth -> birthDate)
)

// ERROR at compile-time (name field is not string)
val e = SchemaEntity.add(DId(Partition.USER))( Props() +
  (PersonSchema.name -> 123) +
  (PersonSchema.age -> 45L) +
  (PersonSchema.birth -> birthDate)
)

```

<br/>
## <a name="features-mapping">Type-safe mapping from/to Scala structure to/from Datomic entities</a>

Based on Scala typeclass conversions and pure functional combinators, we provide this tool to build mappers to convert datomic entities from/to Scala structures such as case classes, tuples or collections.
These conversions are based on previous schema typed attributes.


```scala

case class Person(
  name: String, age: Long
)

object PersonSchema {
  val name = Attribute( person / "name", SchemaType.string, Cardinality.one).withDoc("Person's name")
  val age = Attribute( person / "age", SchemaType.long, Cardinality.one).withDoc("Person's name")
  val birth = Attribute( person / "birth", SchemaType.instant, Cardinality.one).withDoc("Person's birth date")
  ...
}

implicit val personReader = (
  PersonSchema.name.read[String] and 
  PersonSchema.age.read[Long]
)(Person)

DatomicMapping.toEntity(DId(Partition.USER))(
  Person("toto", 30L, birthDate)
)

val entity = database.entity(realEntityId)
val p = DatomicMapping.fromEntity[Person](entity)

```
