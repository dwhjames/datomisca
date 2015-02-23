---
layout: default
title: Building transaction data
---

# <a name="typeclasses">Type safety with type classes (Work in Progress)</a>

## Points in time

The `AsPointT` type class is used when API methods require a point time. With
the Datomic API, time is transaction time as recorded by the transactor.
Points in time can be specified in absolute time as instances of
`java.util.Date`, or as logical time as a basis T value (of type `Long`), or
as an entity id for a transaction entity (of type `Long`).

This type class is used by `Database.asOf`, `Database.since`,
`Database.entidAt`, and `Log.txRange`. For example:

{% highlight scala %}
val db: Database = …
val db1 = db.asOf(1001)
val db2 = db.since(new java.util.Date)
{% endhighlight %}


## Permanent entity ids

The `AsPermanentEntityId` type class is used when API methods require an
identifier for an entity that should be already present in the database. In
Datomic, entities are identified by their `:db/id` values of type `Long`,
however, entities can also be identified by [lookup refs](http://docs.datomic.com/identity.html#lookup-refs).

This type class is used by `Database.entity`, `Database.entid`,
`Database.ident`, `Entity.retract`, `Fact.retract`, `SchemaFact.retract`, and
various excision methods.


## …
