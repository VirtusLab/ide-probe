package org.virtuslab.ideprobe.protocol

import java.nio.file.Path

case class Sdk(
  name: String,
  typeId: String,
  version: Option[String],
  homePath: Option[Path]
)
