---
layout: default
title: Features
---

# <a name="features">Raw API Features</a>

## <a name="features-reactive">Reactive transactions (Asynchronous &amp; Non-Blocking with potential execution isolation)</a>
Using Scala 2.10 Execution Contexts &amp; Futures, Datomic transactions are executed by Datomisca in an asynchronous & non-blocking way managed by the provided execution context. In this way, you can control in which pool of threads you want to execute your transactor requests (communicating with remote Datomic transactor).

```scala
val person = Namespace("person")

Datomic.transact(
  Seq(operation1, operation2, operation3, ...)
) map { tx =>
  ...
}
```

<br/>
## <a name="features-scalatypes">Conversion between Datomic/Clojure and Scala types</a>

When Datomic entities are created or accessed, Datomic types (ie Clojure types) are retrieved. From Java API, all those types are seen as `Object` which is not really useful. So you could end into using `.asInstanceOf[T]` everywhere. Hopefully, Datomisca provides some conversion from/to Datomic types.


```scala
val s: DString = Datomic.toDatomic("toto")
val l: DLong   = Datomic.toDatomic("5L")

val l: String = Datomic.fromDatomic(DString("toto"))
val s: Long   = Datomic.fromDatomic(DLong(5L))

val entity = database.entity(entityId)
val name   = entity.as[String](person / "name")
val age    = entity.as[Long](person / "age")
```

<br/>
## <a name="features-compilequeries">Compile-Time query validation &amp; input/output parameters inference</a>

Based on Scala 2.10 Macros, Datomisca provides :

- **Compile-time validation of Datomic query strings** and syntax error detection.
- **Compile-time inference of input/output parameters** (number for now): when you execute the query, you must pass the right number of input parameters and manage the right number of output parameters.

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

Datomisca is also able to manage:

- datalog rules alias
- query functions
- cherry on cake: you can use Scala valus in query using `${myval}` as in String Interpolation

_In the future, based on type-safe Schema presented below, we will also be able to infer parameter types._


<br/>
## <a name="features-staticqueries">Queries as static reusable structures</a>

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
## <a name="features-ops">Build transaction data programmatically</a>

You can build your operations `add` / `retract` / `addEntity` / `retractEntity` operations in a programmatic way.

```scala
val person = Namespace("person")

Datomic.transact(
  // Atomic Fact ops
  Fact.add(DId(Partition.USER))(person / "name" -> "tata"),
  Fact.retract(DId(Partition.USER))(person / "name" -> "titi"),
  Fact.partition(Partition(Namespace.DB.PART / "mypart")),

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
## <a name="features-schema">Build schemas programmatically</a>

Schema is one of the remarkable specific features of Datomic : schema attributes contrain the type and cardinality of field of Datomic entities. 

Schema attributes are just facts stored in Datomic in a special partition defining the parameters of an attribute: 

- name
- type
- cardinality (one/many)
- doc
- unicity
- fulltext
- ...

In Datomisca, we have provided some helpers to create those attributes in a programmatic way. A Datomic schema is just a sequence of fact operations.

Moreover Datomisca attributes are static-typed and as you can imagine, the attribute type can be used for extended conversion features presented herebelow.

```scala
val uri = "datomic:mem://datomicschemaqueryspec"

val person = new Namespace("person") {
  val character = Namespace("person.character")
}

val violent = AddIdent(person.character / "violent")
val weak    = AddIdent(Keyword(person.character, "weak"))
val clever  = AddIdent(Keyword(person.character, "clever"))
val dumb    = AddIdent(Keyword(person.character, "dumb"))

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
## <a name="features-dtm-parsing">Parse Datomic DTM files at runtime</a>

If you wrote your schema in a DTM file for example, you can load and parse it at runtime.

Naturally doing this, you lose the power of compile-time validation.

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
  val name  = Attribute(
    person / "name",
    SchemaType.string,
    Cardinality.one).withDoc("Person's name")
  val age   = Attribute(
    person / "age",
    SchemaType.long,
    Cardinality.one).withDoc("Person's name")
  val birth = Attribute(
    person / "birth",
    SchemaType.instant,
    Cardinality.one).withDoc("Person's birth date")
}

// OK
SchemaFact.add(DId(Partition.USER))( PersonSchema.name -> "toto" ) 
// ERROR at compile-time since attribute "name" is a string
SchemaFact.add(DId(Partition.USER))( PersonSchema.name -> 123L )   

// OK
val e = SchemaEntity.add(DId(Partition.USER))( Props() +
  (PersonSchema.name  -> "toto") +
  (PersonSchema.age   -> 45L) +
  (PersonSchema.birth -> birthDate)
)

// ERROR at compile-time (field "name" should be a string)
val e = SchemaEntity.add(DId(Partition.USER))( Props() +
  (PersonSchema.name  -> 123) +
  (PersonSchema.age   -> 45L) +
  (PersonSchema.birth -> birthDate)
)

```

<br/>
## <a name="features-mapping">Type-safe mapping from/to Scala structure to/from Datomic entities</a>

Based on Scala typeclass conversions and pure functional combinators, we provide this tool to build mappers to convert datomic entities from/to Scala structures such as case classes, tuples or collections.
These conversions are naturally based on previously described schema typed attributes.


```scala
import Datomic._
import DatomicMapping._

case class Person(
  name: String, age: Long
)

object PersonSchema {
  val name  = Attribute(
    person / "name",
    SchemaType.string,
    Cardinality.one).withDoc("Person's name")
  val age   = Attribute(
    person / "age",
    SchemaType.long,
    Cardinality.one).withDoc("Person's name")
  val birth = Attribute(
    person / "birth",
    SchemaType.instant,
    Cardinality.one).withDoc("Person's birth date")
  ...
}

implicit val personReader = (
  PersonSchema.name .read[String] and
  PersonSchema.age  .read[Long]   and
  PersonSchema.birth.read[java.util.Date]
)(Person)

implicit val personWriter = (
  PersonSchema.name .write[String] and
  PersonSchema.age  .write[Long]   and
  PersonSchema.birth.write[java.util.Date]
)(unlift(Person.unapply))

DatomicMapping.toEntity(DId(Partition.USER))(
  Person("toto", 30L, birthDate)
)

val entity = database.entity(realEntityId)
val p = DatomicMapping.fromEntity[Person](entity)

```
