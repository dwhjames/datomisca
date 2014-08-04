import sbt._

object Dependencies {

  object V {
    val macroParadise = "2.0.1"

    val datomic       = "0.9.4766.16"

    val specs2        = "2.3.12"
    val scalaTest     = "2.1.7"
  }

  object Compile {
    val datomic = "com.datomic"    %    "datomic-free"    %    V.datomic    %    "provided" exclude("org.slf4j", "slf4j-nop")
  }

  object Test {
    val specs2 = "org.specs2"    %%    "specs2"    %    V.specs2    %    "test"
  }

  object IntegrationTest {
    val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest % "it"
  }

}
