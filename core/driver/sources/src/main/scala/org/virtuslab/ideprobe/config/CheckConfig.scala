package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.CheckConfig.ErrorConfig
import org.virtuslab.ideprobe.config.CheckConfig.FreezeConfig

final case class CheckConfig(errors: ErrorConfig, freezes: FreezeConfig)

object CheckConfig {
  case class ErrorConfig(
      enabled: Boolean,
      includeMessages: Seq[String],
      excludeMessages: Seq[String]
  )
  case class FreezeConfig(
      enabled: Boolean
  )
}
