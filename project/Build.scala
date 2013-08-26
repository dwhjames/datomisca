
import scala.language.postfixOps

import sbt._
import Keys._
import sbtunidoc.Plugin._


object DatomiscaBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
      version       := "0.5.1",
      organization  := "com.pellucid",
      scalaVersion  := "2.10.2",
      scalacOptions ++= Seq(
          "-deprecation",
          "-feature",
          "-unchecked"
        )
    )

  lazy val datomisca = Project(
      id       = "datomisca",
      base     = file("."),
      settings = rootProjectSettings
    ) aggregate(common, macros, core, extras, tests)

  lazy val common = Project(
      id       = "common",
      base     = file("common"),
      settings = commonProjectSettings
    )

  lazy val macros = Project(
      id       = "macros",
      base     = file("macros"),
      settings = macrosProjectSettings
    ) dependsOn(common)

  lazy val core = Project(
      id       = "core",
      base     = file("core"),
      settings = coreProjectSettings
    ) dependsOn(common, macros)

  lazy val extras = Project(
      id       = "extras",
      base     = file("extras"),
      settings = extrasProjectSettings
    ) dependsOn(common, core)

  lazy val tests = Project(
      id = "tests",
      base = file("tests"),
      settings = testsProjectSettings
    ) dependsOn(common, core, extras, macros)


  val typesafeRepo = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases"  at "http://repo.typesafe.com/typesafe/releases/"
  )

  val datomicRepo = Seq(
    "clojars"   at "https://clojars.org/repo",
    "couchbase" at "http://files.couchbase.com/maven2"
  )

  lazy val sharedSettings =
    buildSettings ++
    Seq(
      resolvers ++= typesafeRepo ++ datomicRepo,
      libraryDependencies ++= Dependencies.shared,
      shellPrompt := CustomShellPrompt.customPrompt
    )


  lazy val rootProjectSettings =
    sharedSettings ++
    unidocSettings ++
    bintray.Plugin.bintraySettings ++
    Seq(
      name := "datomisca",

      scalacOptions in ScalaUnidoc += "-Ymacro-no-expand",

      publishArtifact in (Compile, packageDoc) := false,

      mappings in (Compile, packageBin) <++= mappings in (common, Compile, packageBin),
      mappings in (Compile, packageBin) <++= mappings in (macros, Compile, packageBin),
      mappings in (Compile, packageBin) <++= mappings in (core,   Compile, packageBin),
      mappings in (Compile, packageBin) <++= mappings in (extras, Compile, packageBin),

      mappings in (Compile, packageSrc) <++= mappings in (common, Compile, packageSrc),
      mappings in (Compile, packageSrc) <++= mappings in (macros, Compile, packageSrc),
      mappings in (Compile, packageSrc) <++= mappings in (core,   Compile, packageSrc),
      mappings in (Compile, packageSrc) <++= mappings in (extras, Compile, packageSrc),

      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      bintray.Keys.bintrayOrganization in bintray.Keys.bintray := Some("pellucid")
    )

  lazy val subProjectSettings =
    sharedSettings ++
    Seq(
      publish      := (),
      publishLocal := ()
    )

  lazy val commonProjectSettings =
    subProjectSettings ++
    Seq(
      name := "Datomisca common"
    )

  lazy val macrosProjectSettings =
    subProjectSettings ++
    Seq(
      name := "Datomisca macros",

      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
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
      name := "Datomisca core",

      (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genCore
    )

  lazy val extrasProjectSettings =
    subProjectSettings ++
    mapGenSourceSettings ++
    Seq(
      name := "Datomisca extras",

      (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genExtras
    )

  lazy val testsProjectSettings =
    subProjectSettings ++
    Seq(
      name := "Datomisca tests",

      libraryDependencies ++= Dependencies.test,

      fork in Test := true
    )

}

object Dependencies {

  object Compile {
    val datomic = "com.datomic"    %    "datomic-free"    %    "0.8.4020.26"    %    "provided" exclude("org.slf4j", "slf4j-nop")
  }
  import Compile._

  object Test {
    val specs2 = "org.specs2"    %%    "specs2"    %    "2.0"    %    "test"
    val junit  = "junit"         %     "junit"     %    "4.8"     %    "test"
  }
  import Test._

  val shared = Seq(datomic)
  val test   = Seq(specs2, junit)
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
