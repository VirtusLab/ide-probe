package org.virtuslab.ideprobe.dependencies

import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import java.net.URI
import org.virtuslab.ideprobe.{Config, ConfigFormat, Id, error}
import pureconfig.{ConfigReader, ConfigSource, ConfigWriter}
import pureconfig.generic.semiauto.{deriveReader, deriveWriter}

sealed trait Plugin

object Plugin extends ConfigFormat {

  case class Bundled(bundle: String) extends Plugin {
    override def toString: String = {
      s"Plugin($bundle)"
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
      val conf = config.source.value.fold(_.toString, _.render(ConfigRenderOptions.concise))
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
    fs.config.source.value.getOrElse(error(s"cannot serialize $fs"))
  }

  implicit val pluginReader: ConfigReader[Plugin] = {
    possiblyAmbiguousAdtReader[Plugin](
      deriveReader[Versioned],
      deriveReader[Direct],
      deriveReader[Bundled],
      fromSourcesReader,
      deriveReader[Empty]
    )
  }

  implicit val pluginWriter: ConfigWriter[Plugin] = {
    possiblyAmbiguousAdtWriter[Plugin](
      deriveWriter[Versioned],
      deriveWriter[Direct],
      deriveWriter[Bundled],
      fromSourcesWriter,
      deriveWriter[Empty]
    )
  }
}
