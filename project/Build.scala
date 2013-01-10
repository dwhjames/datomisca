import sbt._
import Keys._

object BuildSettings {
  val buildName = "reactivedatomic"
  val buildOrganization = "pellucid"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.10.0-RC1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object ApplicationBuild extends Build {

  val typesafeRepo = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  )

//  val datomicCredentials = Credentials(Path.userHome / ".sbt" / ".credentials")

  val datomicRepo = Seq(
    //"Bitbucket.org HTTP" at "https://bitbucket.org/mandubian/datomic-mvn/raw/master/releases/"
    "clojars" at "https://clojars.org/repo",
    "couchbase" at "http://files.couchbase.com/maven2"
  )

  lazy val datomic = Project(
    "datomic", file("."),
    settings = BuildSettings.buildSettings ++ Seq(
      //logLevel := Level.Debug,
      //ivyLoggingLevel := UpdateLogging.Full,
      //scalacOptions ++= Seq("-Xlog-implicits"),
      fork in Test := true,
      //parallelExecution in Test := false,
      //javaOptions in test += "-Xmx512M -Xmx512m -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1024M",
      resolvers ++= typesafeRepo ++ datomicRepo,
      //credentials += datomicCredentials,
      libraryDependencies ++= Seq(
        "com.datomic" % "datomic-free" % "0.8.3731" % "provided" exclude("org.slf4j", "slf4j-nop"),
        "org.scala-lang" % "scala-compiler" % "2.10.0-RC2",
        "org.specs2" % "specs2_2.10.0-RC1" % "1.12.2" % "test",
        "junit" % "junit" % "4.8" % "test"
        /*"org.clojure" % "clojure" % "1.4.0", 
          "org.clojure" % "data.json" % "0.1.2", 
          "net.java.dev.jets3t" % "jets3t" % "0.8.1", 
          "org.hornetq" % "hornetq-core" % "2.2.2.Final", 
          "org.infinispan" % "infinispan-client-hotrod" % "5.1.2.FINAL", 
          "org.apache.lucene" % "lucene-core" % "3.3.0", 
          "com.google.guava" % "guava" % "12.0.1", //dans play
          "spy" % "spymemcached" % "2.8.1", 
          "org.apache.tomcat" % "tomcat-jdbc" % "7.0.27", 
          "postgresql" % "postgresql" % "9.1-901.jdbc4", 
          "org.codehaus.janino" % "commons-compiler-jdk" % "2.6.1",
          "org.slf4j" % "slf4j-api" % "1.6.4",
          "com.h2database" % "h2" % "1.3.165",
          "org.fressian" % "fressian" % "0.6.3",
          "org.scala-lang" % "scala-compiler" % "2.10.0-RC2" */
      )
    )
  )
}
