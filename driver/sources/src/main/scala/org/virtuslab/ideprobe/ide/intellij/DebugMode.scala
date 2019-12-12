package org.virtuslab.ideprobe.ide.intellij

object DebugMode {
  private val enabled: Boolean = env("IDEPROBE_DEBUG", "false").toBoolean
  private val suspend: Boolean = env("IDEPROBE_DEBUG_SUSPEND", "false").toBoolean
  private val port: Int = env("IDEPROBE_DEBUG_PORT", "5005").toInt

  def vmOption: Seq[String] = {
    val suspendOpt = if (suspend) "suspend=y" else "suspend=n"
    val addressOpt = s"address=$port"
    if (enabled) {
      s"-agentlib:jdwp=transport=dt_socket,server=y,$suspendOpt,$addressOpt" :: Nil
    } else {
      Nil
    }
  }

  private def env(name: String, default: String) = System.getenv().getOrDefault(name, default)
}
