import sbt._
import Keys._

object MacroSettings {

  val settings = Seq(
    addCompilerPlugin("org.scalamacros" % "paradise" % Dependencies.V.macroParadise cross CrossVersion.full),

    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),

    libraryDependencies ++= (
        if (scalaVersion.value.startsWith("2.10")) List("org.scalamacros" %% "quasiquotes" % Dependencies.V.macroParadise)
        else Nil
      )
  )
}
