package org.virtuslab.ideprobe.config

import java.nio.file.{Path, Paths}
import org.virtuslab.ideprobe.config.PathsConfig.{cachePath, screenshotsPath, tmpDir}

case class PathsConfig(instances: Path = tmpDir,
                       workspaces: Path = tmpDir,
                       screenshots: Path = screenshotsPath,
                       cache: Path = cachePath)

object PathsConfig {
  private val tmpDir = Paths.get(
    sys.props
      .get("java.io.tmpdir")
      .getOrElse(
        throw new Exception(
          "Temp directory location could not be found. Please specify probe.paths in your config explicitly."
        )
      )
  )
  private val ideprobePath = tmpDir.resolve("ideprobe")
  private val screenshotsPath = ideprobePath.resolve("screenshots")
  private val cachePath = ideprobePath.resolve("cache")

  val Default: PathsConfig = PathsConfig()
}
