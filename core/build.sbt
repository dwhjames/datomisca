
name := "datomisca-core"

libraryDependencies += Dependencies.Compile.datomic

mappings in (Compile, packageSrc) <++=
  (sourceManaged in Compile, managedSources in Compile) map { (base, srcs) =>
    srcs pair (Path.relativeTo(base) | Path.flat)
  }

(sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.genCore

publish := ()

publishLocal := ()

publishArtifact := false
