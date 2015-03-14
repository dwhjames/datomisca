
Defaults.itSettings

name := "datomisca-tests"

libraryDependencies += Dependencies.Compile.datomic

libraryDependencies += Dependencies.IntegrationTest.scalaTest

// add scala-xml dependency when needed (for Scala 2.11 and newer)
// this mechanism supports cross-version publishing
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
    case _ =>
      libraryDependencies.value
  }
}

fork in IntegrationTest := true

publishArtifact := false
