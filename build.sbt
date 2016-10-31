
organization in ThisBuild := "com.github.dwhjames"

licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion in ThisBuild := "2.11.8"

// crossScalaVersions in ThisBuild := Seq("2.11.8", "2.12.0")

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

scalacOptions in ThisBuild ++= (
  if (scalaVersion.value.startsWith("2.10")) Nil
  else List("-Ywarn-unused-import")
)


resolvers in ThisBuild ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.typesafeRepo("releases"),
  "clojars" at "https://clojars.org/repo",
  "couchbase" at "http://files.couchbase.com/maven2"
)

shellPrompt in ThisBuild := CustomShellPrompt.customPrompt

// configure publishing to bintray
bintray.Plugin.bintraySettings

lazy val datomisca = project.
  in(file(".")).
  aggregate(macros, core, tests, integrationTests)

// needed for aggregated build
MacroSettings.settings

libraryDependencies += Dependencies.Compile.datomic

// disable some aggregation tasks for subprojects
aggregate in doc            := false

aggregate in Keys.`package` := false

aggregate in packageBin     := false

aggregate in packageDoc     := false

aggregate in packageSrc     := false

aggregate in publish        := false

aggregate in publishLocal   := false

aggregate in PgpKeys.publishSigned      := false

aggregate in PgpKeys.publishLocalSigned := false


lazy val macros = project in file("macros")

// map macros project classes and sources into root project
mappings in (Compile, packageBin) <++= mappings in (macros, Compile, packageBin)

mappings in (Compile, packageSrc) <++= mappings in (macros, Compile, packageSrc)


lazy val core = project.
  in(file("core")).
  dependsOn(macros)

// map core project classes and sources into root project
mappings in (Compile, packageBin) <++= mappings in (core, Compile, packageBin)

mappings in (Compile, packageSrc) <++= mappings in (core, Compile, packageSrc)


lazy val tests = project.
  in(file("tests")).
  dependsOn(macros, core)


lazy val integrationTests = project.
  in(file("integration")).
  dependsOn(macros, core).
  configs(IntegrationTest)
