package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.CheckConfig.{ErrorConfig, FreezeConfig}


final case class CheckConfig(errors: ErrorConfig = ErrorConfig(), freezes: FreezeConfig = FreezeConfig())

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig()

  case class ErrorConfig(
      enabled: Boolean = false,
      includeMessages: Seq[String] = Seq(".*"),
      excludeMessages: Seq[String] = Nil
  )
  case class FreezeConfig(
      enabled: Boolean = false
  )
}
