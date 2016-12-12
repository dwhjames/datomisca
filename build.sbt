
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

lazy val tests = project.
  in(file("tests")).
  dependsOn(macros, core)

lazy val integrationTests = project.in(file("integration")).
  settings(Defaults.itSettings).
  settings(
    name := "datomisca-tests",
    libraryDependencies ++= Seq(
      Dependencies.Compile.datomic,
      Dependencies.IntegrationTest.scalaTest,
      "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
    ),
    fork in IntegrationTest := true,
    publishArtifact := false
  ).
  dependsOn(macros, core).
  configs(IntegrationTest)


lazy val core = project.in(file("core")).
  settings(
    name := "datomisca-core",
    libraryDependencies += Dependencies.Compile.datomic,
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
    libraryDependencies += Dependencies.Compile.datomic,
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

