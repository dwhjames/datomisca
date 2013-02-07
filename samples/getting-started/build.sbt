name := "datomisca-getting-started"

organization := "pellucidanalytics"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.0"

fork in test := true

resolvers ++= Seq(
  "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
  "clojars" at "https://clojars.org/repo"
)

libraryDependencies ++= Seq(
  "pellucidanalytics" %% "datomisca" % "0.1-SNAPSHOT",
  "com.datomic" % "datomic-free" % "0.8.3789"
    exclude("org.slf4j", "slf4j-nop") 
    exclude("org.jboss.netty", "netty")
)
