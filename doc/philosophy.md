---
layout: default
title: Philosophy
---

# <a name="philosophy">Philosophy</a>

## <a name="philosophy-embrace">Embrace Datomic principles without compromising it</a>
Datomisca is a very thin layer around Java API but we aim at exposing Datomic Clojure functionalities as directly as possible without compromising their essential purpose.

## <a name="philosophy-enhance">Enhance Datomic features with Scala flavors</a>

When meaningful, we enhance Datomic features with Scala specific patterns such as:

- Type-safety, 
- Asynchronism & non-blocking patterns, 
- Functional programming
- Compile-time enhancement with Scala 2.10 Macro

Datomic Java API is also quite poor in terms of typing because it tends to return `List[List[Object]]`.  

In Datomisca, we provide tools to convert those too generic types into Scala generic types.
