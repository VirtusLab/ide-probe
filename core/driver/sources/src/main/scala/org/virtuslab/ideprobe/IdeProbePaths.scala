package org.virtuslab.ideprobe

import java.nio.file.Path
import java.nio.file.Paths

import org.virtuslab.ideprobe.config.PathsConfig

case class IdeProbePaths(
    base: Path,
    instances: Path,
    workspaces: Path,
    screenshots: Path,
    cache: Path,
    trusted: Path,
    logExport: Option[Path]
)

object IdeProbePaths {
  val Default: IdeProbePaths = {
    val config = IntelliJFixture.defaultConfig
    from(config.paths)
  }

  def from(config: PathsConfig): IdeProbePaths = {
    val basePath = config.base.getOrElse(Paths.get(System.getProperty("java.io.tmpdir")).resolve("ide-probe"))
    val instancesPath = config.instances.getOrElse(basePath.resolve("instances"))
    val workspacesPath = config.workspaces.getOrElse(basePath.resolve("workspaces"))
    val screenshotsPath = config.screenshots.getOrElse(basePath.resolve("screenshots"))
    val cachePath = config.cache.getOrElse(basePath.resolve("cache"))
    val trustedPath = config.trusted.getOrElse(Paths.get("/"))
    val logExportPath = config.logExport

    IdeProbePaths(basePath, instancesPath, workspacesPath, screenshotsPath, cachePath, trustedPath, logExportPath)
  }
}
