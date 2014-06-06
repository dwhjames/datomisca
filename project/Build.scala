
import scala.language.postfixOps

import sbt._
import Keys._
import sbtunidoc.Plugin._


object DatomiscaBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
      version       := "0.7-alpha-10",
      organization  := "com.pellucid",
      scalaVersion  := "2.11.1",
      crossScalaVersions := Seq("2.10.4", "2.11.1"),
      scalacOptions ++= Seq(
          // "-deprecation",
          "-feature",
          "-unchecked"
        ),
      addCompilerPlugin("org.scalamacros" % "paradise" % Dependencies.V.macroParadise cross CrossVersion.full)
    )

  lazy val datomisca = Project(
      id       = "datomisca",
      base     = file("."),
      settings = rootProjectSettings
    ) aggregate(macros, core, tests, integrationTests)

  lazy val macros = Project(
      id       = "macros",
      base     = file("macros"),
      settings = macrosProjectSettings
    )

  lazy val core = Project(
      id       = "core",
      base     = file("core"),
      settings = coreProjectSettings
    ) dependsOn(macros)

  lazy val tests = Project(
      id = "tests",
      base = file("tests"),
      settings = testsProjectSettings
    ) dependsOn(core, macros)

  lazy val integrationTests = Project(
      id       = "integrationTests",
      base     = file("integration")
    ) dependsOn (core, macros) configs (IntegrationTest) settings (integrationTestsProjectSettings:_*)


  val repositories = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases"  at "http://repo.typesafe.com/typesafe/releases/",

    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    "clojars" at "https://clojars.org/repo",
    "couchbase" at "http://files.couchbase.com/maven2"
  )

  lazy val sharedSettings =
    buildSettings ++
    Seq(
      resolvers ++= repositories,
      libraryDependencies ++= Dependencies.shared,
      shellPrompt := CustomShellPrompt.customPrompt
    )

  lazy val macroParadiseSettings =
    Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),

      libraryDependencies ++= (
        if (scalaVersion.value.startsWith("2.10")) List("org.scalamacros" %% "quasiquotes" % Dependencies.V.macroParadise)
        else Nil
      )
    )


  lazy val rootProjectSettings =
    sharedSettings ++
    unidocSettings ++
    bintray.Plugin.bintraySettings ++
    macroParadiseSettings ++
    Seq(
      name := "datomisca",

      // disable some aggregation tasks for subprojects
      aggregate in Keys.doc          := false,
      aggregate in Keys.`package`    := false,
      aggregate in Keys.packageBin   := false,
      aggregate in Keys.packageDoc   := false,
      aggregate in Keys.packageSrc   := false,
      aggregate in Keys.publish      := false,
      aggregate in Keys.publishLocal := false,

      // substitue unidoc as the way to generate documentation
      packageDoc in Compile <<= packageDoc in ScalaUnidoc,
      artifact in (ScalaUnidoc, packageDoc) := {
        val previous: Artifact = (artifact in (ScalaUnidoc, packageDoc)).value
        previous.copy(classifier = Some("javadoc"))
      },

      // map subproject classes into root project
      mappings in (Compile, packageBin) <++= mappings in (macros, Compile, packageBin),
      mappings in (Compile, packageBin) <++= mappings in (core,   Compile, packageBin),

      // map subproject sources into root project
      mappings in (Compile, packageSrc) <++= mappings in (macros, Compile, packageSrc),
      mappings in (Compile, packageSrc) <++= mappings in (core,   Compile, packageSrc),

      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      bintray.Keys.bintrayOrganization in bintray.Keys.bintray := Some("pellucid")
    )

  lazy val subProjectSettings =
    sharedSettings ++
    Seq(
      publish      := (),
      publishLocal := ()
    )

  lazy val macrosProjectSettings =
    subProjectSettings ++
    macroParadiseSettings ++
    Seq(
      name := "datomisca-macros"
    )

  lazy val mapGenSourceSettings =
    Seq(
      mappings in (Compile, packageSrc) <++=
        (sourceManaged in Compile, managedSources in Compile) map { (base, srcs) =>
          (srcs x (Path.relativeTo(base) | Path.flat))
        }
    )

  lazy val coreProjectSettings =
    subProjectSettings ++
    mapGenSourceSettings ++
    Seq(
      name := "datomisca-core",

      scalacOptions += "-deprecation",

      (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genCore
    )

  lazy val testsProjectSettings =
    subProjectSettings ++
    Seq(
      name := "datomisca-tests",

      libraryDependencies ++= Dependencies.test,

      fork in Test := true
    )

  lazy val integrationTestsProjectSettings =
    subProjectSettings ++
    Defaults.itSettings ++
    Seq(
      name := "datomisca-tests",

      libraryDependencies ++= Dependencies.integrationTest,

      // add scala-xml dependency when needed (for Scala 2.11 and newer)
      // this mechanism supports cross-version publishing
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
          case _ =>
            libraryDependencies.value
        }
      },

      fork in IntegrationTest := true
    )

}

object Dependencies {

  object V {
    val macroParadise = "2.0.0"

    val datomic       = "0.9.4724"

    val specs2        = "2.3.11"
    val junit         = "4.8"
    val scalaTest     = "2.1.3"
  }

  object Compile {
    val datomic = "com.datomic"    %    "datomic-free"    %    V.datomic    %    "provided" exclude("org.slf4j", "slf4j-nop")
  }
  import Compile._

  object Test {
    val specs2 = "org.specs2"    %%    "specs2"    %    V.specs2    %    "test"
    val junit  = "junit"         %     "junit"     %    V.junit     %    "test"
  }
  import Test._

  object IntegrationTest {
    val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest % "it"
  }
  import IntegrationTest._

  val shared = Seq(datomic)
  val test   = Seq(specs2, junit)
  val integrationTest = Seq(scalaTest)
}

object CustomShellPrompt {

  val Branch = """refs/heads/(.*)\s""".r

  def gitBranchOrSha =
    Process("git symbolic-ref HEAD") #|| Process("git rev-parse --short HEAD") !! match {
      case Branch(name) => name
      case sha          => sha.stripLineEnd
    }

  val customPrompt = { state: State =>

    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data) map { name =>
      "[" + scala.Console.CYAN + name + scala.Console.RESET + "] " +
      scala.Console.BLUE + "git:(" +
      scala.Console.RED + gitBranchOrSha +
      scala.Console.BLUE + ")" +
      scala.Console.RESET + " $ "
    } getOrElse ("> ")

  }
}
