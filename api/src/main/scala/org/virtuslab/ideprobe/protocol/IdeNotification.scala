package org.virtuslab.ideprobe.protocol

final case class IdeNotification(severity: String)

object IdeNotification {
  object Severity {
    val Info = "INFORMATION"
  }
}
