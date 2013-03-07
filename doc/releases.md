---
layout: default
title: Releases
---

# <a name="releases">Releases</a>

You can find the code of our releases tagged in [Github](https://github.com/pellucidanalytics/datomisca/tags).

## <a name="release-0.2">Version 0.2 (2013/03/03)</a>

> _v0.2_ is an important technical release bringing massive API cleaning & internal mechanism improvement.

### Main Enhancements

- **Complete rewrite of Datomic to Scala type conversion mechanism** to make it theoretically purer & very robust.
- **Huge API refactoring & simplification**
- **Global code cleaning**

### Technical environment

_v0.2_ has been tested against [Datomic v0.8.3814](http://downloads.datomic.com/free.html)

To use v0.2, just add following to your SBT config:

```
resolvers ++= Seq(
  // to get Datomisca
  "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
  "datomisca-repo releases"  at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases",
  // to get Datomic free (for pro, you must put in your own repo or local)
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "pellucidanalytics" %% "datomisca" % "0.2",
  "com.datomic" % "datomic-free" % "0.8.3814"
)
```



