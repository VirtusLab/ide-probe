package org.virtuslab.ideprobe.dependencies

import java.nio.file.Paths

import scala.annotation.tailrec

import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.DependenciesConfig

import org.virtuslab.ideprobe.Extensions.PathExtension

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
      "ext" -> version.ext,
      "revision" -> version.releaseOrBuild,
      "build" -> version.build,
      "version" -> version.releaseOrBuild,
      "release" -> version.release.getOrElse("snapshot-release")
    )
    val replacedBeforeResolvingGlobs = replacements.foldLeft(pattern) { case (path, (pattern, replacement)) =>
      path.replace(s"[$pattern]", replacement)
    }
    val replaced = replaceGlobsWithExistingDirectories(List(replacedBeforeResolvingGlobs)).head
    Dependency(replaced)
  }

  // solution below assumes that each * character is used to mark one part of the path (one atomic directory),
  // for example: "file:///home/.cache/ides/com.jetbrains.intellij.idea/ideaIC/[revision]/*/ideaIC-[revision]/"
  @tailrec
  private def replaceGlobsWithExistingDirectories(paths: List[String]): List[String] =
    if (paths.exists(pathMightBeValidResource))
      paths.filter(pathMightBeValidResource)
    else
      replaceGlobsWithExistingDirectories(paths.flatMap(replaceFirstFoundWildcardWithDirectories))

  private def pathMightBeValidResource(path: String): Boolean =
    path.indexOf('*') == -1 &&
      (path.endsWith(".zip") ||
        path.endsWith(".dmg") ||
        Paths.get(path.replace("file:", "")).isDirectory)

  private def replaceFirstFoundWildcardWithDirectories(path: String): List[String] = {
    def removeLastFileSeparator(path: String): String = if (path.endsWith("/")) path.init else path

    val pathUntilWildcard = Paths.get(path.substring(0, path.indexOf('*')).replace("file:", ""))
    val stringPathAfterWildcard = path.substring(path.indexOf('*') + 1)

    if (pathUntilWildcard.exists) {
      pathUntilWildcard
        .directChildren()
        .map { replaced =>
          (if (path.startsWith("file:")) "file:" else "") + removeLastFileSeparator(
            replaced.toString
          ) + stringPathAfterWildcard
        }
    } else {
      Nil
    }
  }
}
