---
layout: default
title: Play Datomisca
---

# Play Datomisca

[Play Datomisca](https://github.com/pellucidanalytics/play-datomisca) is a module for [Play Framework](http://www.playframework.org) that enables you to use Datomisca from within Play in a seamless way.

For now, it’s quite raw and doesn’t provide much on top of Datomisca, but more features will be progressively added, such as help to configure Datomic.

> Play Datomisca is available on GitHub [here](https://github.com/pellucidanalytics/play-datomisca)


## Usage

_v0.5.1_ depends on [Datomisca v0.5.1](https://github.com/pellucidanalytics/datomisca/releases/tag/v0.5.1)

To use v0.5.1, just add following to your SBT config:

```
resolvers ++= Seq(
  // to get Datomisca and Play-Datomisca
  "Pellucid Bintray"  at "http://dl.bintray.com/content/pellucid/maven",
  ...
)

libraryDependencies ++= Seq(
  "com.pellucid" %% "play-datomisca" % "0.5.1",
  "com.datomic" % "datomic-free" % "0.8.4020.26"
)
```
