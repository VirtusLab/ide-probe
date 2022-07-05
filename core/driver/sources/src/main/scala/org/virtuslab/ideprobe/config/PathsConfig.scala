package org.virtuslab.ideprobe.config

import java.nio.file.{Path}

case class PathsConfig(
    base: Option[Path] = None,
    instances: Option[Path] = None,
    workspaces: Option[Path] = None,
    screenshots: Option[Path] = None,
    cache: Option[Path] = None,
    trusted: Option[Path] = None
)
