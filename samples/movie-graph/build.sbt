name := "datomisca-movie-graph"

organization := "pellucidanalytics"

version := "0.3-SNAPSHOT"

scalaVersion := "2.10.0"

resolvers ++= Seq(
  "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
  "datomisca-repo releases"  at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "pellucidanalytics" %% "datomisca" % "0.3-SNAPSHOT",
  "com.datomic" % "datomic-free" % "0.8.4007"
)
