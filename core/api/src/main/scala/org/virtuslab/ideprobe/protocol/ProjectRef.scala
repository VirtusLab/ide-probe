package org.virtuslab.ideprobe.protocol

import com.typesafe.config.ConfigValueFactory
import pureconfig.ConfigReader
import pureconfig.ConfigWriter
import pureconfig.generic.semiauto.deriveReader
import pureconfig.generic.semiauto.deriveWriter

import org.virtuslab.ideprobe.ConfigFormat

sealed trait ProjectRef

object ProjectRef extends ConfigFormat {
  case object Default extends ProjectRef
  case class ByName(name: String) extends ProjectRef

  def apply(name: String): ProjectRef = ByName(name)

  private val defaultProjectRefConfigReader: ConfigReader[Default.type] =
    ConfigReader.fromStringOpt(s => if (s.toLowerCase == "default") Some(Default) else None)

  implicit val projectRefConfigReader: ConfigReader[ProjectRef] = {
    possiblyAmbiguousAdtReader(
      deriveReader[ByName],
      defaultProjectRefConfigReader
    )
  }

  implicit val projectRefConfigWriter: ConfigWriter[ProjectRef] = {
    possiblyAmbiguousAdtWriter(
      deriveWriter[ByName],
      ConfigWriter.fromFunction[Default.type] { _ =>
        ConfigValueFactory.fromAnyRef("default")
      }
    )
  }
}
