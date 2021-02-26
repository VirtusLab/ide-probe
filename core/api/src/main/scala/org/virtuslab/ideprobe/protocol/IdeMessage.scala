package org.virtuslab.ideprobe.protocol

import org.virtuslab.ideprobe.protocol.IdeMessage.Level

case class IdeMessage(level: Level, content: String, pluginId: Option[String]) {
  def isError: Boolean = level == IdeMessage.Level.Error
  def isWarn: Boolean = level == IdeMessage.Level.Warn
  def isInfo: Boolean = level == IdeMessage.Level.Info
}

object IdeMessage {
  sealed trait Level
  object Level {
    case object Error extends Level
    case object Warn extends Level
    case object Info extends Level
    case object Other extends Level
  }
}
