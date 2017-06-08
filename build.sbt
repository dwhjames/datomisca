import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._

organization in ThisBuild := "com.quartethealth"
licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1")

val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

resolvers in ThisBuild ++= Seq(
  "clojars" at "https://clojars.org/repo"
)

lazy val datomisca = project.
  in(file(".")).
  aggregate(macros, core, tests, integrationTests)

lazy val tests = project.in(file("tests")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-tests",
    libraryDependencies ++= Seq(
      datomic,
      specs2
    ),
    fork in Test := true,
    publishArtifact := false
  ).
  dependsOn(macros, core)

lazy val integrationTests = project.in(file("integration")).
  settings(noPublishSettings).
  settings(Defaults.itSettings).
  settings(
    name := "datomisca-tests",
    libraryDependencies ++= Seq(
      datomic,
      scalatest,
      xmlModule
    ),
    fork in IntegrationTest := true,
    publishArtifact := false
  ).
  dependsOn(macros, core).
  configs(IntegrationTest)

lazy val core = project.in(file("core")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-core",
    libraryDependencies += datomic,
    (sourceGenerators in Compile) += ((sourceManaged in Compile) map Boilerplate.genCore).taskValue
  ).
  dependsOn(macros)

lazy val macros = project.in(file("macros")).
  settings(noPublishSettings).
  settings(
    name := "datomisca-macros",
    addCompilerPlugin(paradise),
    libraryDependencies ++= Seq(
      datomic,
      reflect(scalaVersion.value)
    )
  )

lazy val docs = project.in(file("docs")).
  settings(
    name := "Datomisca Docs",
    moduleName := "datomisca-docs"
  ).
  settings(docSettings).
  settings(noPublishSettings).
  settings(addCompilerPlugin(paradise)).
  dependsOn(core, macros).
  enablePlugins(MicrositesPlugin)

val baseSettings = Seq(
  scalacOptions ++= compilerOptions
)

val docSettings = baseSettings ++ Seq(
  micrositeName := "Datomisca",
  micrositeDescription := "Scala API for Datomic",
  micrositeAuthor := "Daniel James",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "https://xxx",
  micrositeBaseUrl := "datomisca",
  micrositeDocumentationUrl := "api",
  micrositeGithubOwner := "flyingwalrusllc",
  micrositeGithubRepo := "datomisca",
  micrositePalette := Map(
    "brand-primary" -> "#5B5988",
    "brand-secondary" -> "#292E53",
    "brand-tertiary" -> "#222749",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"
  ),
  // addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  ghpagesNoJekyll := false,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-doc-root-content", (resourceDirectory.in(Compile).value / "rootdoc.txt").getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:quartethealth/datomisca.git",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

mappings in (Compile, packageBin) ++= (mappings in (macros, Compile, packageBin)).value
mappings in (Compile, packageSrc) ++= (mappings in (macros, Compile, packageSrc)).value

mappings in (Compile, packageBin) ++= (mappings in (core, Compile, packageBin)).value
mappings in (Compile, packageSrc) ++= (mappings in (core, Compile, packageSrc)).value

val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

def datomic = "com.datomic" % "datomic-free" % "0.9.5561" % Provided
def specs2 = "org.specs2" %% "specs2-core" % "3.8.8" % Test
def scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "it"
def xmlModule = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
def paradise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
def reflect(vers: String)  = "org.scala-lang" % "scala-reflect" % vers
