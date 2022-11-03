package org.virtuslab.ideprobe.config

import scala.concurrent.duration._

import org.virtuslab.ideprobe.config.DriverConfig.DebugConfig
import org.virtuslab.ideprobe.config.DriverConfig.LaunchParameters
import org.virtuslab.ideprobe.config.DriverConfig.XvfbConfig

case class DriverConfig(
    launch: LaunchParameters,
    check: CheckConfig,
    display: String,
    xvfb: XvfbConfig,
    vmOptions: Seq[String],
    env: Map[String, String],
    debug: DebugConfig
)

object DriverConfig {
  case class LaunchParameters(
      command: Seq[String],
      timeout: FiniteDuration
  )
  case class XvfbConfig(screen: ScreenConfig)
  case class ScreenConfig(
      width: Int,
      height: Int,
      depth: Int
  )

  case class DebugConfig(
      enabled: Boolean,
      port: Int,
      suspend: Boolean
  )
}
