package org.virtuslab.ideprobe.dependencies

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

  def resolver(artifact: String): DependencyResolver[IntelliJVersion] = DependencyResolver { version =>
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
    val artifactWithReplacementsApplied = replacements.foldLeft(pattern) { case (path, (pattern, replacement)) =>
      path.replace(s"[$pattern]", replacement)
    }

    if (artifactWithReplacementsApplied.startsWith("file:"))
      findFilesMatchingGlob(artifactWithReplacementsApplied.stripPrefix("file:"))
        .map("file:" + _)
        .map(Dependency.apply)
        .getOrElse(Dependency.Missing)
    else
      Dependency(artifactWithReplacementsApplied)
  }

  // The solution below assumes that each * character is used to mark one part of the path (one atomic directory),
  // for example: "file:///home/.cache/ides/com.jetbrains.intellij.idea/ideaIC/[revision]/*/ideaIC-[revision]/".
  // Wildcard character should NOT be used as the last element of the pattern - to avoid ambiguous results.
  // Works only for files in local filesystem.
  private def findFilesMatchingGlob(pathPattern: String): Option[String] = {
    findFilesMatchingGlobRecursively(
      Paths.get(pathPattern).getRoot,
      pathPattern.split(File.separatorChar).filter(_.nonEmpty).toList
    ).headOption
  }

  private def findFilesMatchingGlobRecursively(accumulatedPath: Path, remainingSegments: List[String]): List[String] = {
    remainingSegments match {
      case "*" :: tail =>
        accumulatedPath.directChildren().flatMap(child => findFilesMatchingGlobRecursively(child, tail))
      case head :: tail if accumulatedPath.resolve(head).exists =>
        findFilesMatchingGlobRecursively(accumulatedPath.resolve(head), tail)
      case _ :: _ =>
        Nil
      case Nil =>
        List(accumulatedPath.toString)
    }
  }
}
