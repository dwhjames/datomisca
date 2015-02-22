import sbtunidoc.Plugin._
import sbtunidoc.Plugin.UnidocKeys._
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
    "-doc-source-url", s"https://github.com/dwhjames/datomisca/tree/v${version.value}€{FILE_PATH}.scala")


autoAPIMappings := true

apiURL := Some(url("https://dwhjames.github.io/datomisca/api/current/"))

apiMappings += {
  val jarFiles = (dependencyClasspath in Compile).value.files
  def findJarFile(s: String) = jarFiles.find(file => file.toString.contains(s)).get
  val datomicJarFile = findJarFile("com.datomic/datomic-free")
  (datomicJarFile -> url("http://docs.datomic.com/javadoc/"))
}

lazy val transformJavaDocLinksTask = taskKey[Unit](
  "Transform JavaDoc links - replace #java.io.File with ?java/io/File.html"
)

transformJavaDocLinksTask := {
  val log = streams.value.log
  log.info("Transforming JavaDoc links")
  val t = (target in unidoc).value
  (t ** "*.html").get.filter(hasJavadocApiLink).foreach { f =>
    log.info("Transforming " + f)
    val newContent = javadocApiLink.replaceAllIn(IO.read(f), m =>
      "href=\"" + m.group(1) + "?" + m.group(2).replace(".", "/") + ".html")
    IO.write(f, newContent)
  }
}

val javadocApiLink = """href=\"(http://docs\.datomic\.com/javadoc/index\.html)#([^"]*)""".r

def hasJavadocApiLink(f: File): Boolean = (javadocApiLink findFirstIn IO.read(f)).nonEmpty

transformJavaDocLinksTask <<= transformJavaDocLinksTask triggeredBy (unidoc in Compile)
