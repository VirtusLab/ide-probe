package org.virtuslab.ideprobe

import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.config.PathsConfig

case class IdeProbePaths(instances: Path, workspaces: Path, screenshots: Path, cache: Path)

object IdeProbePaths {
  val TemporaryPaths: IdeProbePaths = {
    from(PathsConfig())
  }

  def from(config: PathsConfig): IdeProbePaths = {
    IdeProbePaths(config.base, config.instances, config.workspaces, config.screenshots, config.cache)
  }

  def apply(base: Option[Path],
            instances: Option[Path],
            workspaces: Option[Path],
            screenshots: Option[Path],
            cache: Option[Path]): IdeProbePaths = {
    val basePath = base.getOrElse(Paths.get(System.getProperty("java.io.tmpdir")))
    val instancesPath = instances.getOrElse(basePath.resolve("instances"))
    val workspacesPath = workspaces.getOrElse(basePath.resolve("workspaces"))
    val screenshotsPath = screenshots.getOrElse(basePath.resolve("screenshots"))
    val cachePath = cache.getOrElse(basePath.resolve("cache"))

    IdeProbePaths(instancesPath, workspacesPath, screenshotsPath, cachePath)
  }
}
