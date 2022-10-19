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
    val replaced =
      if (replacedBeforeResolvingGlobs.startsWith("file:"))
        resolveGlobsInPattern(replacedBeforeResolvingGlobs)
      else
        replacedBeforeResolvingGlobs

    Dependency(replaced)
  }

  private def resolveGlobsInPattern(pathPattern: String): String =
    replaceGlobsWithExistingDirectories(List(pathPattern), pathPattern).head

  // Solution below assumes that each * character is used to mark one part of the path (one atomic directory),
  // for example: "file:///home/.cache/ides/com.jetbrains.intellij.idea/ideaIC/[revision]/*/ideaIC-[revision]/".
  // Wildcard character should NOT be used as the last element of the pattern - to avoid ambiguous results.
  // Works only for files in local filesystem.
  @tailrec
  private def replaceGlobsWithExistingDirectories(paths: List[String], originalPattern: String): List[String] =
    if (paths.exists(pathMightBeValidResource(_, originalPattern)))
      paths.filter(pathMightBeValidResource(_, originalPattern))
    else
      replaceGlobsWithExistingDirectories(paths.flatMap(replaceFirstFoundWildcardWithDirectories), originalPattern)

  private def pathMightBeValidResource(candidatePath: String, originalPattern: String): Boolean =
    !candidatePath.contains('*') && // below - make sure that candidatePath's last path element is same as in pattern
      candidatePath.substring(candidatePath.lastIndexOf('/')) == originalPattern.substring(
        originalPattern.lastIndexOf('/')
      ) && Paths.get(candidatePath.replace("file:", "")).exists

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
