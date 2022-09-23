package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.DependenciesConfig

trait IntelliJResolver {
  def community: DependencyResolver[IntelliJVersion]
  def ultimate: DependencyResolver[IntelliJVersion]
}

object IntelliJResolver {
  def fromConfig(config: DependenciesConfig.Resolvers): Seq[DependencyResolver[IntelliJVersion]] =
    config.intellij.repositories.flatMap { pattern =>
      if (Set("official", "default").contains(pattern.toLowerCase)) {
        val officialRepositoriesPatterns = IntelliJFixture.defaultConfig.resolvers.intellij.repositories
        officialRepositoriesPatterns.map { repositoryPattern =>
          IntelliJPatternResolver(repositoryPattern).resolver("ideaIC")
        }
      } else
        Seq(IntelliJPatternResolver(pattern).resolver("ideaIC"))
    }
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
      "ext" -> version.ext.get, // .get will be OK since we have `ext = ".zip"` in the reference.conf file
      "revision" -> version.releaseOrBuild,
      "build" -> version.build,
      "version" -> version.releaseOrBuild,
      "release" -> version.release.getOrElse("snapshot-release")
    )
    val replaced = replacements.foldLeft(pattern) { case (path, (pattern, replacement)) =>
      path.replace(s"[$pattern]", replacement)
    }
    Dependency(replaced)
  }
}
