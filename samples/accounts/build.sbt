name := "datomisca-accounts-sample"

organization := "pellucidanalytics"

version := "0.2-SNAPSHOT"

scalaVersion := "2.10.0"

fork in test := true

resolvers ++= Seq(
  "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "pellucidanalytics" %% "datomisca" % "0.2-SNAPSHOT",
  "com.datomic" % "datomic-free" % "0.8.3789"
)
