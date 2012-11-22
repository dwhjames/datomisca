name := "reactive-datomic"

organization := "zenexity"

version := "0.1-SNAPSHOT"

publishMavenStyle := true

scalaVersion := "2.10.0-RC2"

//fork in test := true

parallelExecution in Test := false

//javaOptions in test += "-Xmx8ghfghfdsfghG"

resolvers += "JBoss" at "https://repository.jboss.org/nexus/content/groups/public"

libraryDependencies ++= Seq(
  "org.clojure" % "clojure" % "1.4.0", 
  "org.clojure" % "data.json" % "0.1.2", 
  "net.java.dev.jets3t" % "jets3t" % "0.8.1", 
  "org.hornetq" % "hornetq-core" % "2.2.2.Final", 
  "org.infinispan" % "infinispan-client-hotrod" % "5.1.2.FINAL", 
  "org.apache.lucene" % "lucene-core" % "3.3.0", 
  "com.google.guava" % "guava" % "12.0.1", //dans play
  "spy" % "spymemcached" % "2.8.1", 
  "org.apache.tomcat" % "tomcat-jdbc" % "7.0.27", 
  "postgresql" % "postgresql" % "9.1-901.jdbc4", 
  "org.codehaus.janino" % "commons-compiler-jdk" % "2.6.1",
  "org.specs2" % "specs2_2.10.0-RC1" % "1.12.2" % "test",
  "junit" % "junit" % "4.8" % "test",
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "com.h2database" % "h2" % "1.3.165",
  "org.fressian" % "fressian" % "0.6.3",
  "org.scala-lang" % "scala-compiler" % "2.10.0-RC2" 
)
