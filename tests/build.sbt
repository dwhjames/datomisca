
name := "datomisca-tests"

libraryDependencies += Dependencies.Compile.datomic

libraryDependencies += Dependencies.Test.specs2

fork in Test := true
