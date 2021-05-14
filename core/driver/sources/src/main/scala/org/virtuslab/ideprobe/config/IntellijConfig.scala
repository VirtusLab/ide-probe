package org.virtuslab.ideprobe.config

import java.nio.file.Path

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin, Resource}
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

sealed trait IntellijConfig {
  def plugins: Seq[Plugin]
}

object IntellijConfig extends ConfigFormat {
  def apply(): IntellijConfig = Default()
  case class Default(
      version: IntelliJVersion = IntelliJVersion.Latest,
      plugins: Seq[Plugin] = Seq.empty
  ) extends IntellijConfig

  case class Existing(
      path: Path,
      plugins: Seq[Plugin] = Seq.empty
  ) extends IntellijConfig

  implicit val intelliJConfigReader: ConfigReader[IntellijConfig] = {
    possiblyAmbiguousAdtReader[IntellijConfig](
      deriveReader[Existing],
      deriveReader[Default]
    )
  }
}
