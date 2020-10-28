package org.virtuslab.ideprobe.config

import java.nio.file.Path

case class PathsConfig(base: Option[Path],
                       instances: Option[Path],
                       workspaces: Option[Path],
                       screenshots: Option[Path],
                       cache: Option[Path])

object PathsConfig {
  def apply(): PathsConfig = {
    PathsConfig(None, None, None, None, None)
  }
}
