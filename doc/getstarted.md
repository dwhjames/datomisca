---
layout: default
title: Getting Started
---

# Getting Started

Here is a very simple sample to get started with Datomisca.

## Requirements

- SBT 0.13.x
- Scala 2.10.4

## Github project

You can find this sample in Datomic Github Samples [Getting-Started](https://github.com/dwhjames/datomisca/tree/master/samples/getting-started)


## #1 Add resolvers to SBT

You can add that in your `build.sbt` or `Build.scala` depending on your choice.

{% highlight scala %}
resolvers += Resolver.bintrayRepo("dwhjames", "maven")
// to get Datomic free (for pro, you must put in your own repo or local)
resolvers += "clojars" at "https://clojars.org/repo"
{% endhighlight %}

## #2 Add dependencies

> The latest release is {{ site.latestrelease }}

{% highlight scala %}
libraryDependencies ++= Seq(
  "com.github.dwhjames" %% "datomisca" % "{{ site.latestrelease }}",
  "com.datomic" % "datomic-free" % "0.9.4724"
)
{% endhighlight %}

## #3 Add imports

The following imports should be sufficient to get you started.

{% highlight scala %}
import scala.concurrent.ExecutionContext.Implicits.global

import datomisca._
{% endhighlight %}


## #4 Create a connection

To use Datomisca, you need an implicit connection to Datomic in your scope.

{% highlight scala %}
// Datomic URI definition
val uri = "datomic:mem://datomisca-getting-started"

// Datomic Connection as an implicit in scope
implicit val conn = Datomic.connect(uri)
{% endhighlight %}

> Datomic’s public API is threadsafe, and there is no need to pool the Datomic connection. Datomic will return the same instance of Connection for a given URI, no matter how many times you ask.  And Datomic will cache that single instance even if you don't. ([Stuart Halloway 2013-06-25](https://groups.google.com/d/msg/datomic/ekwfTZbMCaE/GL4J0AyonI8J))


## #5 Create a database

We start from scratch so let's first create a DB.

{% highlight scala %}
Datomic.createDatabase(uri)
{% endhighlight %}

> This method returns a boolean. If true, then a fresh database was created, or else a database already existed for the given URI.


## #6 Create a schema

Datomisca allows to define your Schema in a programmatic way.

Here you create your:

- namespaces, 
- attributes
- enumerated entities

Attributes and enumerated entites are gathered in a schema representing your entity.

Let's create four attributes to represent a `Person`:

- `name:       SchemaType.string,  Cardinality.one`
- `home:       SchemaType.string,  Cardinality.one`
- `birth:      SchemaType.instant, Cardinality.one`
- `hobbies:    SchemaType.ref,     Cardinality.many`

{% highlight scala %}
object PersonSchema {
  // Namespaces definition to be reused in Schema
  object ns {
    val person = new Namespace("person") {
      val hobby = Namespace("person.hobby")
    }
  }

  // Attributes
  val name = Attribute(
    ns.person / "name",
    SchemaType.string,
    Cardinality.one).withDoc("Person's name")
  val home = Attribute(
    ns.person / "home",
    SchemaType.string,
    Cardinality.one).withDoc("Person's hometown")
  val birth = Attribute(
    ns.person / "birth",
    SchemaType.instant,
    Cardinality.one).withDoc("Person's birth date")
  val hobbies = Attribute(
    ns.person / "hobbies",
    SchemaType.ref,
    Cardinality.many).withDoc("Person's hobbies")

  // hobby enumerated values
  val movies  = AddIdent(ns.person.hobby / "movies")
  val music   = AddIdent(ns.person.hobby / "music")
  val reading = AddIdent(ns.person.hobby / "reading")
  val sports  = AddIdent(ns.person.hobby / "sports")
  val travel  = AddIdent(ns.person.hobby / "travel")

  // Schema
  val txData: Seq[TxData] = Seq(
    name, home, birth, characters, // attributes
    movies, music, reading, sports, travel // ident entities
  )

}

{% endhighlight %}

 - `Namespace` is just a helper making our code clearer when creating keywords.
 - `Attribute` and `AddIdent` are helpers for creating Datomic schema data.
 - the `PersonSchema` and `ns` objects are our idiom for gathering schema information, but feel free to organize your schemas as you see fit.


## #7 Transact your schema

Now we have a schema, let's transact it into our database. This is our first
operation using the transactor and as you may know, Datomisca manages
transactor's communication in an asynchronous and non-blocking way based on
Scala 2.10 Execution Context.

To ask the transaction to perform some operations, we use the following method:

{% highlight scala %}
Datomic.transact(txData: TraversableOnce[TxData])(implicit conn: Connection, ec: ExecutionContext): Future[TxReport]
{% endhighlight %}

As you can see, it accepts a collection of transaction data and returns a `Future[TxReport]`.

> If you are unfamilar with Scala Future, then consult [this overview](http://docs.scala-lang.org/overviews/core/futures.html).

So let's transact our schema into Datomic:

{% highlight scala %}
Datomic.transact(PersonSchema.txData) flatMap { tx =>
  ...
  // do something
  ...
}
{% endhighlight %}

> We use `flatMap` because we expect to perform other asynchronous operations upon the completion of the transaction.


## #8 Define your first entity

The following code will construct the transaction data for a person called
John, whose hometown is Brooklyn, was born on Jan, 1 1980, and likes
travelling and watching movies.

{% highlight scala %}
// John temporary ID
val johnId = DId(Partition.USER)
// John person entity
val john: TxData = (
    SchemaEntity.newBuilder
      += (PersonSchema.name  -> "John")
      += (PersonSchema.home  -> "Brooklyn, NY")
      += (PersonSchema.birth -> new java.util.Date(80, 0, 1))
      ++= (PersonSchema.hobbies -> Set(PersonSchema.movies, PersonSchema.travel))
  ) withId johnId
{% endhighlight %}

The transaction data `john` is equivalent to the following Clojure map.

{% highlight clojure %}
(let [johnId (d/tempid :db.part/user)]
  {:db/id (d/tempid :db.part/user)
   :person/name "John"
   :person/home "Brooklyn, NY"
   :person/birth (java.util.Date 80 0 1)
   :person/hobbies [:person.hobby/movies :person.hobby/travel]})
{% endhighlight %}

In Datomisca, the `DId` type is one of the ways of constructing entity
ids, and here we are constructing a temporary entity id in the user partition.

The `SchemaEntity` builder follows Scala’s `Builder` for collections. This is
an idiom for incrementally building collections. To build up transaction data
for a new entity, we use attribute–value pairs, rather than keyword–value
pairs. This provides a level of type-safety, as the attribute stores the
schema type and the cardinality along with the keyword ident. The value of the
pair is statically checked against the attribute’s type and cardinality.


## #9 Transact your entity

Transacting regular data and schema data is no different.

{% highlight scala %}
// creates an entity
Datomic.transact(john) map { tx =>
  val realJohnId = tx.resolve(johnId)
  ...
  // Do something else
}
{% endhighlight %}

- `tx.resolve(johnId)` is used to retrieve the real Id after insertion from temporary Id.
- you can also retrieve several ID at the same time:

{% highlight scala %}
val Seq(realId1, realId2, realId3) = tx.resolve(id1, id2, id3)
{% endhighlight %}


## #10 Write a query

So now that we have an entity in our DB, let's try to query for it.

In Datomisca, you **write your queries in Datalog exactly in the same way as
Clojure**. Leveraging Scala’s 2.10 macros, Datomisca **validates the syntax of
your query at compile-time and also deduces the number of input/output
parameters** (more features are also in the roadmap).

Let's write a "find person by name" query:

{% highlight scala %}
val queryFindByName = Query("""
  [:find ?e ?home
   :in $ ?name
   :where
   [?e :person/name ?name]
   [?e :person/home ?home]]
""")
{% endhighlight %}

- This creates a query that accepts two input parameters (`$` and `?age`) and returning two ouput parameters (`?e` and `?home`)
- the query is a static structure and you can declare it once and reuse it as much as you want.
- `$` identifies the database as an input data source
- `?name` is our "by name" input parameter

Datomisca’s query macro also supports
[string interpolation](http://docs.scala-lang.org/overviews/core/string-interpolation.html),
which means that the query can be written as follows.

{% highlight scala %}
val queryFindByName = Query(s"""
  [:find ?e ?home
   :in $$ ?name
   :where
   [?e ${PersonSchema.name} ?name]
   [?e ${PersonSchema.home} ?home]]
""")
{% endhighlight %}

Remember to watch out for escaping the datasource `$` as `$$`. The `toString`
method is called on the values of expressions that are interpolated. The
string representation of attributes is their keyword, which is why we can
rewrite the query this way. The query treats expressions of type `String`
specially, by double quoting them, so,

{% highlight scala %}
val name = "John"
val queryFindByName = Query(s"""
  [:find ?e ?home
   :in $$
   :where
   [?e ${PersonSchema.name} $name]
   [?e ${PersonSchema.home} ?home]]
""")
{% endhighlight %}

will result in a query with a clause

{% highlight clojure %}
[?e :person/name "John"]
{% endhighlight %}


## #11 Execute a query

Queries are executed using the `Datomic.q` method, with your query and the appropriate input parameters.

{% highlight scala %}
val results = Datomic.q(queryFindByName, conn.database, "John")
{% endhighlight %}

- `Datomic.q` expects a query and the right number of input parameters according to your query (here two)
- `conn.database` is the ‘current’ database value available from the connection `conn`.


## #12 Use the query result

The query results are bound to the name `results`.
According to the input query, the compiler has inferred that there should be two output parameters.

Thus, `results` is a `Iterable[(Any, Any)]`.

{% highlight scala %}
results.headOption map {
  case (eid: Long, home: String) =>
    ...
    // do something
}
{% endhighlight %}

Note that results is a `Iterable[(Any, Any)]` and not `Iterable[(Long, String)]`
as you might hope. Why? Because with the info provided in the query,
it's impossible to infer those types directly. In the future, we hope to
extend the power of the query macro to provide type-safety for output
parameters using schema information. Therefore, for now, you must type match with
a `case`.


## #13 Traverse the entity graph

With the previous query, we retrieved `eid`, which is an entity id, and now we
can get the entity from the database and inspect it.

{% highlight scala %}
val entity: Entity = conn.database.entity(eid)
{% endhighlight %}

As before, `conn.database` retrieves the currently available value of the
database, and the `entity` method looks up the entity map for a given
identifier.

The [Entity]({{ site.baseurl }}/api/0.7.x/index.html#datomisca.Entity)
and
[RichEntity]({{ site.baseurl }}/api/0.7.x/index.html#datomisca.package$$RichEntity)
apis provide various ways of interact with entities. The `apply` method
on the implicit `RichEntity` allows us to use attributes rather than
keywords to retrieve values, in a similar fashion to how we constructed
transaction data above.

{% highlight scala %}
val johnName: String = entity(PersonSchema.name)
val johnHome: String = entity(PersonSchema.home)
val johnBirth: java.util.Date = entity(PersonSchema.birth)
{% endhighlight %}

The attributes possess the type information, so Datomisca computes the correct
return type.

Datomisca is able to do this for all primitives, of cardinality one or many, but
it can’t do this for reference attributes as Datomic will return values of type
`Entity` in most cases, but `Keyword` if the referenced entity has an ident
attribute, which is the case here:

{% highlight scala %}
val johnHobbies = entity.read[Set[Keyword]](PersonSchema.hobbies)
{% endhighlight %}

The `read` method allows us to do a type-safe cast.


## And much more…

Read the more detailed guides and the [API docs]({{ site.baseurl }}/api/0.7.x/index.html) for more details about what was covered here.
