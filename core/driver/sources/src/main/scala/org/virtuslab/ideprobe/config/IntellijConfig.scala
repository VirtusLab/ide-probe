package org.virtuslab.ideprobe.config

import java.nio.file.Path

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.Plugin

sealed trait IntellijConfig {
  def plugins: Seq[Plugin]
}

object IntellijConfig extends ConfigFormat {
  case class Default(
      version: IntelliJVersion,
      plugins: Seq[Plugin]
  ) extends IntellijConfig

  case class Existing(
      path: Path,
      plugins: Seq[Plugin]
  ) extends IntellijConfig

  implicit val intelliJConfigReader: ConfigReader[IntellijConfig] = {
    possiblyAmbiguousAdtReader[IntellijConfig](
      deriveReader[Existing],
      deriveReader[Default]
    )
  }
}
