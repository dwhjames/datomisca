---
layout: default
title: Releases
---

# <a name="releases">Releases</a>

You can find tagged releases described here and at [Github](https://github.com/pellucidanalytics/datomisca/releases).

## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.5.1" name="release-0.5.1">Version 0.5.1 (2013/07/16)</a>

> Maintenance release to version 0.5.
> Includes:
> [#66](https://github.com/pellucidanalytics/datomisca/issues/66)
> [#65](https://github.com/pellucidanalytics/datomisca/issues/65)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.5" name="release-0.5">Version 0.5 (2013/07/10)</a>

> Includes:
> [#63](https://github.com/pellucidanalytics/datomisca/issues/63)
> [#61](https://github.com/pellucidanalytics/datomisca/issues/61)
> [#60](https://github.com/pellucidanalytics/datomisca/issues/60)
> [#58](https://github.com/pellucidanalytics/datomisca/issues/58)
> [#57](https://github.com/pellucidanalytics/datomisca/issues/57)
> [#56](https://github.com/pellucidanalytics/datomisca/issues/56)
> [#55](https://github.com/pellucidanalytics/datomisca/issues/55)
> [#54](https://github.com/pellucidanalytics/datomisca/issues/54)
> [#52](https://github.com/pellucidanalytics/datomisca/issues/52)
> [#51](https://github.com/pellucidanalytics/datomisca/issues/51)
> [#44](https://github.com/pellucidanalytics/datomisca/issues/44)
> [#42](https://github.com/pellucidanalytics/datomisca/issues/42)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/0.2" name="release-0.2">Version 0.2 (2013/03/03)</a>

> _v0.2_ is an important technical release bringing massive API cleaning & internal mechanism improvement.

- **Complete rewrite of Datomic to Scala type conversion mechanism** to make it theoretically purer & very robust.
- **Huge API refactoring & simplification**
- **Global code cleaning**

## Usage

_v0.5.1_ has been tested against [Datomic v0.8.4020.26](http://downloads.datomic.com/free.html)

To use v0.5.1, just add following to your SBT config:

```
resolvers ++= Seq(
  // to get Datomisca
  "Pellucid Bintray"  at "http://dl.bintray.com/content/pellucid/maven",
  // to get Datomic free (for pro, you must put in your own repo or local)
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.pellucid" %% "datomisca" % "0.5.1",
  "com.datomic" % "datomic-free" % "0.8.4020.26"
)
```



