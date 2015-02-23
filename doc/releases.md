---
layout: default
title: Releases
---

# <a name="releases">Releases</a>

You can find tagged releases described here and at [Github](https://github.com/pellucidanalytics/datomisca/releases).


## Usage

To use Datomisca, just add following to your SBT config:

{% highlight scala %}
resolvers ++= Seq(
  // to get Datomisca
  resolvers += Resolver.bintrayRepo("dwhjames", "maven"),
  // to get Datomic free (for pro, you must put in your own repo or local)
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.github.dwhjames" %% "datomisca" % "{{ site.latestrelease }}",
  "com.datomic" % "datomic-free" % "0.9.4724"
)
{% endhighlight %}
