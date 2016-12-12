import sbt._
import Keys._

object MacroSettings {

  val settings = Seq(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )
}
