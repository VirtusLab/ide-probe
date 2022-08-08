package org.virtuslab.ideprobe.dependencies

import java.net.URI

import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigRenderOptions
import pureconfig.ConfigReader
import pureconfig.ConfigSource
import pureconfig.ConfigWriter
import pureconfig.generic.semiauto.deriveReader
import pureconfig.generic.semiauto.deriveWriter

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.Id
import org.virtuslab.ideprobe.error

sealed trait Plugin

object Plugin extends ConfigFormat {

  case class Bundled(bundle: String) extends Plugin {
    override def toString: String = {
      s"Plugin($bundle)"
    }
  }
  case class BundledCrossVersion(bundle: String, scalaVersion: String, version: String) extends Plugin {
    override def toString: String = {
      s"Plugin(${bundle}_$scalaVersion-$version)"
    }
  }

  case class Versioned(id: String, version: String, channel: Option[String]) extends Plugin {
    override def toString: String = {
      s"Plugin($id:$version${channel.fold("")(":" + _)})"
    }
  }

  case class Direct(uri: URI) extends Plugin {
    override def toString: String = {
      s"Plugin($uri)"
    }
  }

  case class FromSources(id: Id, config: Config) extends Plugin {
    override def toString: String = {
      val conf = config.source.value().fold(_.toString, _.render(ConfigRenderOptions.concise))
      s"Plugin($id, $conf)"
    }
  }
  private[ideprobe] case class Empty() extends Plugin

  def apply(id: String, version: String, channel: Option[String] = None): Versioned = {
    Versioned(id, version, channel)
  }

  implicit val fromSourcesReader: ConfigReader[FromSources] = ConfigReader.fromFunction { cv =>
    val source = ConfigSource.fromConfig(cv.asInstanceOf[ConfigObject].toConfig)
    source.at("id").load[Id].map { id =>
      FromSources(id, Config(source))
    }
  }

  implicit val fromSourcesWriter: ConfigWriter[FromSources] = ConfigWriter.fromFunction { fs =>
    fs.config.source.value().getOrElse(error(s"cannot serialize $fs"))
  }

  implicit val pluginReader: ConfigReader[Plugin] = {
    possiblyAmbiguousAdtReader[Plugin](
      deriveReader[Versioned],
      deriveReader[Direct],
      deriveReader[Bundled],
      deriveReader[BundledCrossVersion],
      fromSourcesReader,
      deriveReader[Empty]
    )
  }

  implicit val pluginWriter: ConfigWriter[Plugin] = {
    possiblyAmbiguousAdtWriter[Plugin](
      deriveWriter[Versioned],
      deriveWriter[Direct],
      deriveWriter[Bundled],
      deriveWriter[BundledCrossVersion],
      fromSourcesWriter,
      deriveWriter[Empty]
    )
  }
}
