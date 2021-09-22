package org.virtuslab.ideprobe.dependencies

trait IntelliJResolver {
  def community: DependencyResolver[IntelliJVersion]
  def ultimate: DependencyResolver[IntelliJVersion]
}

case class IntelliJPatternResolver(pattern: String) extends IntelliJResolver {
  def community: DependencyResolver[IntelliJVersion] = resolver("ideaIC")
  def ultimate: DependencyResolver[IntelliJVersion] = resolver("ideaIU")

  def resolver(artifact: String): DependencyResolver[IntelliJVersion] = { version =>
    val replacements = Map(
      "organisation" -> "com.jetbrains.intellij",
      "orgPath" -> "com/jetbrains/intellij",
      "module" -> "idea",
      "artifact" -> artifact,
      "revision" -> version.releaseOrBuild,
      "build" -> version.build,
      "version" -> version.releaseOrBuild,
      "release" -> version.release.getOrElse("snapshot-release")
    )
    val replaced = replacements.foldLeft(pattern) {
      case (path, (pattern, replacement)) => path.replace(s"[$pattern]", replacement)
    }
    Dependency(replaced)
  }
}
