package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.DependenciesConfig

trait IntelliJResolver

object IntelliJResolver {
  def fromConfig(config: DependenciesConfig.IntelliJ): Seq[DependencyResolver[IntelliJVersion]] =
    config.repositories.flatMap { pattern =>
      if (Set("official", "default").contains(pattern.toLowerCase)) {
        val probeConfigFromReference = IntelliJFixture.readIdeProbeConfig(Config.fromReferenceConf, "probe")
        val officialRepositoriesPatterns = probeConfigFromReference.resolvers.intellij.repositories
        officialRepositoriesPatterns.map(repositoryPattern => IntelliJPatternResolver(repositoryPattern).resolver)
      } else
        Seq(IntelliJPatternResolver(pattern).resolver)
    }
}

case class IntelliJPatternResolver(pattern: String) extends IntelliJResolver {

  def resolver: DependencyResolver[IntelliJVersion] = { version =>
    val replacements = Map(
      "format" -> version.format.get, // .get will be OK since we have `format = ".zip"` in the reference.conf file
      "revision" -> version.releaseOrBuild,
      "build" -> version.build,
      "version" -> version.releaseOrBuild
    )
    val replaced = replacements.foldLeft(pattern) { case (path, (pattern, replacement)) =>
      path.replace(s"[$pattern]", replacement)
    }
    Dependency(replaced)
  }

}
