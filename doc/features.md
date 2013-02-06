---
layout: default
title: Features
---

# <a name="features">Raw API Features</a>

## <a name="features-reactive">Reactive Transactor API (Asynchronous &amp; Non-Blocking with potential execution isolation)</a>
Using Scala 2.10 Execution Contexts &amp; Futures, Datomic transactions are executed by Datomisca in an asynchronous & non-blocking way managed by the provided execution context. In this way, you can control in which pool of threads you want to execute your remote transactor requests.


## <a name="features-compilequeries">Compile-Time query validation &amp; input/output parameters inference</a>

Based on Scala 2.10 Macros, Datomisca is able :
- **to validate Datomic query strings at compile-time** and then detect where there are errors.
- **to infer the number of input/output parameters** so when you execute the query  , you must pass the right number of input parameters and manage the right number of output parameters.

_In the future, based on type-safe Schema presented below, we will also be able to infer parameter types._

```scala    
    Query("""
      [ :find ?e ?n 
        :in $ ?char
        :where  [ ?e :person/name ?n ] 
                [ ?e :person/character ?char ]
      ]
    """)

    // produces a Query with :
    //   - 2 input arguments (db and ?char)
    //   - 2 output arguments (?e ?n)
```

<br/>
## <a name="features-staticqueries">Query as static reusable structures</a>

This is a very important idea in Datomic: **A query is a static structure** which can be built once and reused as many times as you want.

```scala
    val query = Query("""
      [ :find ?e ?n 
        :in $ ?char
        :where  [ ?e :person/name ?n ] 
                [ ?e :person/character ?char ]
      ]
    """)
      
    Datomic.q(
      query, 
      database, 
      DRef(KW(":person.character/violent"))
    ).map {
      case (e: DLong, n: DString) => // 
    }
```

<br/>
# <a name="features">Extended Features</a>

## <a name="features-schema">Static-typed &amp; programmatic Schema definition API</a>

Schema is one of the remarkable specific features of Datomic as it enables contraints on the type and cardinality of the inserted data.  
Schema attributes are just facts stored in Datomic in a special partition defining the parameters of an attribute : 
As Scala is static-typed language, it seems really logical to link those attributes to 

## <a name="features-ops">Programmatic &amp; type-safe Datomic operations</a>

Based on previously described static-typed schema, you can build your operations `add` / `retract` / `addEntity` / `retractEntity` operations in a type-safe way.


## <a name="features-mapping">Type-safe mapping from/to Scala structure to/from Datomic entities</a>

Based on Scala typeclass conversions and pure functional combinators, we provide this tool to build mappers to convert datomic entities from/to Scala structures such as case classes, tuples or collections.

