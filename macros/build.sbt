
name := "datomisca-macros"

MacroSettings.settings

libraryDependencies += Dependencies.Compile.datomic

unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / s"scala_${scalaBinaryVersion.value}"

publish := ()

publishLocal := ()
