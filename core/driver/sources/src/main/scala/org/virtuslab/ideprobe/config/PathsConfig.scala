package org.virtuslab.ideprobe.config

import java.nio.file.{Path, Paths}

case class PathsConfig(
    base: Option[Path] = None,
    instances: Option[Path] = None,
    workspaces: Option[Path] = None,
    screenshots: Option[Path] = sys.env.get("IDEPROBE_SCREENSHOTS_DIR").map(path => Paths.get(path)),
    cache: Option[Path] = None,
    trusted: Option[Path] = None
)
