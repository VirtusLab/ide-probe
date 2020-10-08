package org.virtuslab.ideprobe.dependencies

import java.net.URI
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Id
import pureconfig.{ConfigReader, ConfigWriter}
import pureconfig.generic.semiauto.{deriveReader, deriveWriter}

sealed trait Plugin

object Plugin extends ConfigFormat {
  case class Bundled(bundle: String) extends Plugin
  case class Versioned(id: String, version: String, channel: Option[String]) extends Plugin
  case class Direct(uri: URI) extends Plugin
  case class FromSources(id: Id, repository: SourceRepository) extends Plugin
  private[ideprobe] case class Empty() extends Plugin

  def apply(id: String, version: String, channel: Option[String] = None): Versioned = {
    Versioned(id, version, channel)
  }

  implicit val pluginReader: ConfigReader[Plugin] = {
    possiblyAmbiguousAdtReader[Plugin](
      deriveReader[Versioned],
      deriveReader[Direct],
      deriveReader[Bundled],
      deriveReader[FromSources],
      deriveReader[Empty]
    )
  }

  implicit val pluginWriter: ConfigWriter[Plugin] = {
    possiblyAmbiguousAdtWriter[Plugin](
      deriveWriter[Versioned],
      deriveWriter[Direct],
      deriveWriter[Bundled],
      deriveWriter[FromSources],
      deriveWriter[Empty]
    )
  }
}
