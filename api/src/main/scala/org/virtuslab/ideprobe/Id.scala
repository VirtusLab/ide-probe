package org.virtuslab.ideprobe

import com.typesafe.config.ConfigValueFactory
import pureconfig.ConfigCursor
import pureconfig.ConfigReader
import pureconfig.ConfigWriter

sealed trait Id
object Id {
  def apply(name: String): Id = Symbolic(name.toLowerCase)

  final case class Symbolic(name: String) extends Id

  implicit val pluginReader: ConfigReader[Id] = {
    case cursor: ConfigCursor => cursor.asString.map(Id.apply)
  }

  implicit val pluginWriter: ConfigWriter[Id] = {
    case Symbolic(name) => ConfigValueFactory.fromAnyRef(name)
  }
}
