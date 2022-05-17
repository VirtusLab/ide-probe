package org.virtuslab.ideprobe.log

import org.virtuslab.ideprobe.protocol.IdeMessage

case class Message(content: Option[String], throwable: Option[Throwable], level: IdeMessage.Level) {

  def throwableText: Option[String] = {
    throwable.map(_.getMessage.mkString("\n"))
  }

  def render: String = {
    (content ++ throwableText).mkString("\n\n")
  }
}
