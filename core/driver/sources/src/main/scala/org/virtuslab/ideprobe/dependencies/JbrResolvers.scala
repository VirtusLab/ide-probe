package org.virtuslab.ideprobe.dependencies

import java.io.StringReader
import java.nio.file.Path
import java.util.Properties

import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.DependenciesConfig
import org.virtuslab.ideprobe.dependencies.Dependency.Missing

import org.virtuslab.ideprobe.Extensions.PathExtension

object JbrResolvers {
  /*
  The `jbr_dcevm` pattern applies only to versions of IntelliJ older than 2022.2 release, whereas
  `jbr` pattern applies to newer versions where DCEVM is bundled by default.
  Another important note: currently we are using only JBR packages with `x64` architecture support. During tests
  it turned out that using `aarch64` packages on processors with `aarch64` architecture (Mac M1 Pro chip) results
  in "Cannot load JVM bundle:" error. For some reason only `x64` packages work well (both for `x64` and `aarch64`
  processor architectures). Hence `x64` should stay hardcoded in the `officialJbrVersionPatterns` elements.
   */
  private val officialJbrVersionPatterns = Seq(
    "https://cache-redirector.jetbrains.com/intellij-jbr/jbr_dcevm-[major]-[platform]-x64-b[minor].tar.gz",
    "https://cache-redirector.jetbrains.com/intellij-jbr/jbr-[major]-[platform]-x64-b[minor].tar.gz"
  )

  val official: Seq[JbrPatternResolver] = officialJbrVersionPatterns.map(JbrPatternResolver)

  def fromConfig(config: DependenciesConfig.Jbr): Seq[DependencyResolver[Path]] = {
    val fromConfig = config.repositories
      .flatMap(pattern =>
        if (Set("official", "default").contains(pattern.toLowerCase)) official
        else Seq(JbrPatternResolver(pattern))
      )
    if (fromConfig.isEmpty) official else fromConfig
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
