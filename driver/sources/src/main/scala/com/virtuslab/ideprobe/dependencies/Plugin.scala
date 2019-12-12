package com.virtuslab.ideprobe.dependencies

import java.net.URI
import com.virtuslab.ideprobe.ConfigFormat
import com.virtuslab.ideprobe.Id
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

sealed trait Plugin

object Plugin extends ConfigFormat {
  case class Bundled(bundle: String) extends Plugin
  case class Versioned(id: String, version: String, channel: Option[String]) extends Plugin
  case class Direct(uri: URI) extends Plugin
  case class FromSources(id: Id, repository: SourceRepository) extends Plugin

  def apply(id: String, version: String, channel: Option[String] = None): Versioned = {
    Versioned(id, version, channel)
  }

  implicit val pluginReader: ConfigReader[Plugin] = {
    possiblyAmbiguousAdtReader[Plugin](
      deriveReader[Versioned],
      deriveReader[Direct],
      deriveReader[Bundled],
      deriveReader[FromSources]
    )
  }
}
