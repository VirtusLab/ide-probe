package org.virtuslab.ideprobe.config

import scala.concurrent.duration._

import org.virtuslab.ideprobe.config.DriverConfig.LaunchParameters
import org.virtuslab.ideprobe.config.DriverConfig.XvfbConfig

case class DriverConfig(
    launch: LaunchParameters,
    check: CheckConfig,
    xvfb: XvfbConfig,
    headless: Boolean,
    vmOptions: Seq[String],
    env: Map[String, String]
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
}
