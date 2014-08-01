import sbtunidoc.Plugin._, UnidocKeys._
import scala.util.matching.Regex.Match


// substitue unidoc as the way to generate documentation
unidocSettings

packageDoc in Compile <<= packageDoc in ScalaUnidoc

artifact in (ScalaUnidoc, packageDoc) := {
  val previous: Artifact = (artifact in (ScalaUnidoc, packageDoc)).value
  previous.copy(classifier = Some("javadoc"))
}

scalacOptions in (Compile, doc) ++=
  Seq(
    "-implicits",
    "-sourcepath", baseDirectory.value.getAbsolutePath,
    "-doc-source-url", s"https://github.com/pellucidanalytics/datomisca/tree/v${version.value}€{FILE_PATH}.scala")


autoAPIMappings := true

apiURL := Some(url("https://pellucidanalytics.github.io/datomisca/api/current/"))

apiMappings += {
  val jarFiles = (managedClasspath in Compile).value.files
  val datomicJarFile = jarFiles.find(file => file.toString.contains("com.datomic/datomic-free")).get
  (datomicJarFile -> url("http://docs.datomic.com/javadoc/"))
}

lazy val transformJavaDocLinksTask = taskKey[Unit](
  "Transform JavaDoc links - replace #java.io.File with ?java/io/File.html"
)

transformJavaDocLinksTask := {
  val log = streams.value.log
  log.info("Transforming JavaDoc links")
  val t = (target in (ScalaUnidoc, unidoc)).value
  (t ** "*.html").get.filter(hasJavadocApiLink).foreach { f =>
    log.info("Transforming " + f)
    val newContent = javadocApiLink.replaceAllIn(IO.read(f), transformJavaDocLinks)
    IO.write(f, newContent)
  }
}

val transformJavaDocLinks: Match => String = m =>
    "href=\"" + m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"

val javadocApiLink = """href=\"(http://docs\.datomic\.com/javadoc/index\.html)#([^"]*)""".r

def hasJavadocApiLink(f: File): Boolean = (javadocApiLink findFirstIn IO.read(f)).nonEmpty

transformJavaDocLinksTask <<= transformJavaDocLinksTask triggeredBy (unidoc in ScalaUnidoc)
