package org.virtuslab.ideprobe.dependencies

import java.io.StringReader
import java.nio.file.Path
import java.util.Properties
import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe.{Config, OS, error}

case class JbrPatternResolver(pattern: String) extends IntelliJResolver {
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

class JbrResolver extends DependencyResolver[Path] {

  val BASE_URL = "https://cache-redirector.jetbrains.com/intellij-jbr"

  override def resolve(dep: Path): Dependency = {
    val (major, minor) = splitVersion(extractVersionFromIdea(dep))
    Dependency(buildJbrDlUrl(major, minor))
  }

  private def splitVersion(version: String): (String, String) = {
    val lastIndexOfB = version.lastIndexOf('b')
    if (lastIndexOfB > -1)
      version.substring(0, lastIndexOfB) -> version.substring(lastIndexOfB + 1)
    else {
      throw new IllegalStateException(s"Malformed jbr version: $version")
    }
  }

  private def buildJbrDlUrl(major: String, minor: String) = {
    val platform = OS.Current match {
      case OS.Windows => "windows"
      case OS.Unix    => "linux"
      case OS.Mac     => "osx"
    }
    s"$BASE_URL/jbr_dcevm-$major-$platform-x64-b$minor.tar.gz"
  }

  private def extractVersionFromIdea(ideaInstallationDir: Path): String = {
    val dependenciesFile = ideaInstallationDir.resolve("dependencies.txt").content()
    val props = new Properties()
    props.load(new StringReader(dependenciesFile))
    Option(props.getProperty("jdkBuild")).getOrElse(error("Failed to extract JBR version from IntelliJ"))
  }

}
