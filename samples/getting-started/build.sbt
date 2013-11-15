name := "datomisca-getting-started"

organization := "pellucidanalytics"

version := "0.7-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "Pellucid Bintray"  at "http://dl.bintray.com/content/pellucid/maven",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.pellucid" %% "datomisca" % "0.7-SNAPSHOT",
  "com.datomic" % "datomic-free" % "0.8.4260"
)
