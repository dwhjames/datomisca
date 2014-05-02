---
layout: default
title: Releases
---

# <a name="releases">Releases</a>

You can find tagged releases described here and at [Github](https://github.com/pellucidanalytics/datomisca/releases).


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-10" name="release-v0.7-alpha-10">Version 0.7-alpha-10 (2014-04-21)</a>

> Includes:
> [#106](https://github.com/pellucidanalytics/datomisca/issues/106)
> [#107](https://github.com/pellucidanalytics/datomisca/issues/107)
> [#108](https://github.com/pellucidanalytics/datomisca/issues/108)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-9" name="release-v0.7-alpha-9">Version 0.7-alpha-9 (2014-03-17)</a>

> Includes:
> [#104](https://github.com/pellucidanalytics/datomisca/issues/104)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-8" name="release-v0.7-alpha-8">Version 0.7-alpha-8 (2014-02-16)</a>

> Includes:
> [#103](https://github.com/pellucidanalytics/datomisca/issues/103)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-7" name="release-v0.7-alpha-7">Version 0.7-alpha-7 (2014-02-03)</a>

This release fixes the broken release _0.7-alpha-6_
> Includes:
> [#103](https://github.com/pellucidanalytics/datomisca/issues/103)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-5" name="release-v0.7-alpha-5">Version 0.7-alpha-5 (2014-01-06)</a>

> Includes:
> [#100](https://github.com/pellucidanalytics/datomisca/issues/100)
> [#101](https://github.com/pellucidanalytics/datomisca/issues/101)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-4" name="release-v0.7-alpha-4">Version 0.7-alpha-4 (2013-12-19)</a>

> Includes:
> [#99](https://github.com/pellucidanalytics/datomisca/issues/99)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-3" name="release-v0.7-alpha-3">Version 0.7-alpha-3 (2013-12-16)</a>

> Includes:
> [#95](https://github.com/pellucidanalytics/datomisca/issues/95)
> [#97](https://github.com/pellucidanalytics/datomisca/issues/97)
> [#98](https://github.com/pellucidanalytics/datomisca/issues/98)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-2" name="release-v0.7-alpha-2">Version 0.7-alpha-2 (2013-12-12)</a>

> Includes:
> [#93](https://github.com/pellucidanalytics/datomisca/issues/93)
> [#94](https://github.com/pellucidanalytics/datomisca/issues/94)
> [#96](https://github.com/pellucidanalytics/datomisca/issues/96)


## <a href="https://github.com/pellucidanalytics/datomisca/releases/tag/v0.7-alpha-1" name="release-v0.7-alpha-1">Version 0.7-alpha-1 (2013-11-21)</a>

> Includes:
> [#91](https://github.com/pellucidanalytics/datomisca/issues/91)


## Usage

_0.7-alpha-10_ has been tested against [Datomic 0.9.4724](http://downloads.datomic.com/free.html)

To use 0.7-alpha-10, just add following to your SBT config:

```
resolvers ++= Seq(
  // to get Datomisca
  "Pellucid Bintray"  at "http://dl.bintray.com/content/pellucid/maven",
  // to get Datomic free (for pro, you must put in your own repo or local)
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.pellucid" %% "datomisca" % "0.7-alpha-10",
  "com.datomic" % "datomic-free" % "0.9.4724"
)
```



