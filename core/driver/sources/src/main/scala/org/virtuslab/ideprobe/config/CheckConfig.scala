package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.CheckConfig.ErrorConfig


final case class CheckConfig(errors: ErrorConfig = ErrorConfig(), freezes: Boolean = false)

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig()

  case class ErrorConfig(
      enabled: Boolean = false,
      includeMessages: Seq[String] = Seq(".*"),
      excludeMessages: Seq[String] = Nil
  )
}
