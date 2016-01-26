name := "datomisca-getting-started"

organization := "com.github.dwhjames"

version := "0.7-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.bintrayRepo("dwhjames", "maven"),
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "com.github.dwhjames" %% "datomisca" % "0.7.0",
  "com.datomic" % "datomic-free" % "0.9.5344"
)
