package org.virtuslab.ideprobe.dependencies

import java.io.StringReader
import java.nio.file.Path
import java.util.Properties

import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.DependenciesConfig
import org.virtuslab.ideprobe.dependencies.Dependency.Missing

import org.virtuslab.ideprobe.Extensions.PathExtension

object JbrResolvers {
  lazy val official: Seq[JbrPatternResolver] =
    IntelliJFixture.defaultIdeProbeConfig.resolvers.jbr.repositories.map(JbrPatternResolver)

  def fromConfig(config: DependenciesConfig.Jbr): Seq[DependencyResolver[Path]] =
    config.repositories.flatMap { pattern =>
      if (Set("official", "default").contains(pattern.toLowerCase)) {
        val officialJbrRepositoriesPatterns = IntelliJFixture.defaultIdeProbeConfig.resolvers.jbr.repositories
        officialJbrRepositoriesPatterns.map(repositoryPattern => JbrPatternResolver(repositoryPattern))
      } else Seq(JbrPatternResolver(pattern))
    }
}

case class JbrPatternResolver(pattern: String) extends DependencyResolver[Path] {

  override def resolve(path: Path): Dependency = {
    extractVersionFromInstalledIntelliJ(path) match {
      case Some((major, minor)) =>
        val platform = OS.Current match {
          case OS.Windows => "windows"
          case OS.Unix    => "linux"
          case OS.Mac     => "osx"
        }

        val replacements = Map(
          "major" -> major,
          "minor" -> minor,
          "platform" -> platform
        )

        val replaced = replacements.foldLeft(pattern) { case (path, (pattern, replacement)) =>
          path.replace(s"[$pattern]", replacement)
        }

        Dependency(replaced)
      case _ => Missing
    }
  }

  private def extractVersionFromInstalledIntelliJ(ideaInstallationDir: Path): Option[(String, String)] = {
    val dependenciesFile = ideaInstallationDir.resolve("dependencies.txt")
    if (dependenciesFile.toFile.exists()) {
      val props = new Properties()
      props.load(new StringReader(dependenciesFile.content()))
      val version = Option(props.getProperty("jdkBuild"))
        .getOrElse(error("Failed to extract JBR version from IntelliJ"))

      Option(toMinorMajor(version))
    } else {
      None
    }
  }

  private def toMinorMajor(version: String): (String, String) = {
    val bLocation = version.lastIndexOf('b')
    if (bLocation > -1) {
      (version.substring(0, bLocation), version.substring(bLocation + 1))
    } else {
      error(s"Unexpected JBR version found in IntelliJ instance: $version")
    }
  }

}
