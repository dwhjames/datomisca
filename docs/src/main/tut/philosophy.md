---
layout: docs
title: Philosophy
---

# <a name="philosophy">Philosophy</a>

## <a name="philosophy-embrace">Datomic principles, without compromise</a>
Datomisca is a thin layer around Datomic aimed at exposing Datomic’s functionality and leveraging its full power.

The key Datomic features we really love are:

- A database as an immutable value
- An explicit notion of time
- Atomically creating and updating facts
- A navigatable, lazy entity structure
- A Datalog query language with rules
- Queries as reusable static structures
- Schemas to constrain datatypes
- Bidirectional references
- Database temporal exploration
- Database state simulation without commit

In our design, we are also deeply aware of the architecture of Datomic:

- Single, remote, asynchronous transactor
- Multiple, distributed peers with local cache


## <a name="philosophy-enhance">Datomic features with a Scala flavor</a>

Datomisca uses Scala features to enhance the Datomic experience for Scala developers:

- Type safety
- Asynchronicity & non-blocking patterns
- Advanced functional programming
- Compile-time enhancement with Scala 2.10 macros

Other than the small handful of Datomic specific types, Datomic’s Java API specifies parameter and return types almost all as `Object`s, `List`s, and `Map`s.
For example, the result of a query has type `List[List[Object]]`, signifying a set of heterogenously-typed tuples.
In Datomisca, we provide the means to recover the dynamic types of these objects into static Scala types.

An important feature of Datomic is that entities have no specific representation beyond a set of datoms with a common identity. The consequence is that it is really **easy to manipulate data in an atomic way**.

In designing Datomisca, we wanted to preseve this property. That’s why **we don't focus on mapping entities to and from Scala case classes**. Nonetheless, we provide the means to construct mappings as an extension. We caution the keen case-class-mapper to see case classes as a logical view on a collection of datoms, rather than datoms corresponding to an object model.
