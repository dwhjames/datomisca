name := "datomisca-accounts-sample"

organization := "pellucidanalytics"

version := "0.5-SNAPSHOT"

scalaVersion := "2.10.2"

fork in test := true

resolvers ++= Seq(
  "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
  "datomisca-repo releases"  at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "pellucidanalytics" %% "datomisca" % "0.5-SNAPSHOT",
  "com.datomic" % "datomic-free" % "0.8.4020.26"
)
