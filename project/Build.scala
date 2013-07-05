import sbt._
import Keys._

object BuildSettings {
  val buildName              = "datomisca"
  val buildOrganization      = "pellucidanalytics"
  val buildVersion           = "0.3-SNAPSHOT"
  val buildScalaVersion      = "2.10.2"

  val datomicVersion         = "0.8.4007"
 
  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization    := buildOrganization,
    version         := buildVersion,
    scalaVersion    := buildScalaVersion,
    scalacOptions   ++= Seq(
        //"-Xlog-implicits",
        "-deprecation",
        "-feature"
      )
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

  val commonSettings = BuildSettings.buildSettings ++ Seq(
      resolvers ++= typesafeRepo ++ datomicRepo,
      libraryDependencies ++= Seq(
          "com.datomic" % "datomic-free" % BuildSettings.datomicVersion % "provided" exclude("org.slf4j", "slf4j-nop")
        )
    )

  lazy val datomisca = Project(
      BuildSettings.buildName,
      file("."),
      settings = commonSettings ++ Seq(
          fork in Test := true,
          libraryDependencies ++= Seq(
            "org.specs2" %% "specs2" % "1.13" % "test",
            "junit" % "junit" % "4.8" % "test"
          )
        )
    ) dependsOn(common, macros, core, extras)

  lazy val common = Project(
      "common",
      file("common"),
      settings = commonSettings
    )

  lazy val macros = Project(
      "macros",
      file("macros"),
      settings = commonSettings ++ Seq(
        libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
      )
    ) dependsOn(common)

  lazy val core = Project(
      "core",
      file("core"),
      settings = commonSettings ++ Seq(
          (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genCore
        )
    ) dependsOn(common, macros)

  lazy val extras = Project(
      "extras",
      file("extras"),
      settings = commonSettings ++ Seq(
          (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genExtras
        )
    ) dependsOn(common, core)

/*
  lazy val datomic = Project(
    BuildSettings.buildName, file("."),
    settings = BuildSettings.buildSettings ++ Seq(
      (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen,
      //logLevel := Level.Debug,
      //ivyLoggingLevel := UpdateLogging.Full,
      scalacOptions ++= Seq(
        //"-Xlog-implicits",
        //"-deprecation",
        //"-feature"
      ),
      fork in Test := true,
      //parallelExecution in Test := false,
      //javaOptions in test += "-Xmx512M -Xmx512m -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1024M",
      resolvers ++= typesafeRepo ++ datomicRepo,
      //credentials += datomicCredentials,
      libraryDependencies ++= Seq(
        "com.datomic" % "datomic-free" % BuildSettings.datomicVersion % "provided" exclude("org.slf4j", "slf4j-nop"),        "org.scala-lang" % "scala-compiler" % "2.10.0",
        "org.specs2" %% "specs2" % "1.13" % "test",
        "junit" % "junit" % "4.8" % "test"
      ),
      publishMavenStyle := true,
      publishTo <<= version { (version: String) =>
        val localPublishRepo = "../datomisca-repo/"
        if(version.trim.endsWith("SNAPSHOT"))
          Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
        else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
      }
    )
  )
*/
}
