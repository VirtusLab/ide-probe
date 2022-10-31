package org.virtuslab.ideprobe.ide.intellij

import org.virtuslab.ideprobe.config.DriverConfig.DebugConfig

object DebugMode {
  def vmOption(debugMode: DebugConfig): Seq[String] = {
    if (debugMode.enabled) {
      val suspendOpt = if (debugMode.suspend) "suspend=y" else "suspend=n"
      val addressOpt = s"address=${debugMode.port}"
      s"-agentlib:jdwp=transport=dt_socket,server=y,$suspendOpt,$addressOpt" :: Nil
    } else {
      Nil
    }
  }
}
