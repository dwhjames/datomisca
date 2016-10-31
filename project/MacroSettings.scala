import sbt._
import Keys._

object MacroSettings {

  val settings = Seq(
    addCompilerPlugin("org.scalamacros" % "paradise" % Dependencies.V.macroParadise cross CrossVersion.full),

    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
  )
}
