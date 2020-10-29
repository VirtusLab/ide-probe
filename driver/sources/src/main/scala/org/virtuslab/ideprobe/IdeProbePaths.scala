package org.virtuslab.ideprobe

import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.config.PathsConfig

case class IdeProbePaths(base: Path, instances: Path, workspaces: Path, screenshots: Path, cache: Path)

object IdeProbePaths {
  val Default: IdeProbePaths = {
    from(PathsConfig())
  }

  def from(config: PathsConfig): IdeProbePaths = {
    val basePath = config.base.getOrElse(Paths.get(System.getProperty("java.io.tmpdir")))
    val instancesPath = config.instances.getOrElse(basePath.resolve("instances"))
    val workspacesPath = config.workspaces.getOrElse(basePath.resolve("workspaces"))
    val screenshotsPath = config.screenshots.getOrElse(basePath.resolve("screenshots"))
    val cachePath = config.cache.getOrElse(basePath.resolve("cache"))

    IdeProbePaths(basePath, instancesPath, workspacesPath, screenshotsPath, cachePath)
  }
}
