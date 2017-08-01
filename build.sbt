
inThisBuild(List(
  name := "Datomisca",
  organization := "llc.flyingwalrus",
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  scalaVersion := "2.12.3"
))

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

lazy val integrationTests = (project in file("integration")).
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
  settings(
    name := "datomisca-core",
    baseSettings,
    libraryDependencies += datomic,
    (sourceGenerators in Compile) += ((sourceManaged in Compile) map Boilerplate.genCore).taskValue
  ).
  dependsOn(macros)

lazy val macros = project.in(file("macros")).
  settings(
    name := "datomisca-macros",
    addCompilerPlugin(paradise),
    baseSettings,
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

val docSettings = baseSettings ++ Seq()

val publishSettings = Seq()

val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

def datomic = "com.datomic" % "datomic-free" % "0.9.5561.54" % Provided
def specs2 = "org.specs2" %% "specs2" % "2.4.17" % Test
def scalatest = "org.scalatest" %% "scalatest" % "3.0.3" % "it"
def xmlModule = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
def paradise = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch
def reflect(vers: String)  = "org.scala-lang" % "scala-reflect" % vers
