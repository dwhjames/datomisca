import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._

organization in ThisBuild := "com.github.dwhjames"
licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
scalaVersion in ThisBuild := "2.11.8"
crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.1")

scalacOptions in ThisBuild ++= Seq(
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
  settings(
    name := "datomisca-core",
    libraryDependencies += datomic,
    (sourceGenerators in Compile) += ((sourceManaged in Compile) map Boilerplate.genCore).taskValue,
    publish := (),
    publishLocal := (),
    publishArtifact := false
  ).
  dependsOn(macros)

lazy val macros = project.in(file("macros")).
  settings(MacroSettings.settings).
  settings(
    name := "datomisca-macros",
    libraryDependencies += datomic,
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

lazy val docSettings = unidocSettings ++ Seq(
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
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-skip-packages", "scalaz",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-doc-root-content", (resourceDirectory.in(Compile).value / "rootdoc.txt").getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:flyingwalrusllc/datomisca.git",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md",
  ghpagesNoJekyll := false
)

val publishSettings = Seq(
)

val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

def datomic = "com.datomic" % "datomic-free" % "0.9.5130" % Provided exclude("org.slf4j","slf4j-nop")
def specs2 = "org.specs2" %% "specs2" % "2.3.12" % Test
def scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "it"
def xmlModule = "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

