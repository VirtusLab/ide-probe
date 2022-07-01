package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.DriverConfig.{LaunchParameters, XvfbConfig}

import scala.concurrent.duration._

case class DriverConfig(
    launch: LaunchParameters = LaunchParameters(),
    check: CheckConfig = CheckConfig(),
    xvfb: XvfbConfig = XvfbConfig(),
    headless: Boolean = false,
    vmOptions: Seq[String] = Nil,
    env: Map[String, String] = Map.empty
)

object DriverConfig {
  case class LaunchParameters(
      command: Seq[String] = Nil,
      timeout: FiniteDuration = 30.seconds
  )
  case class XvfbConfig(screen: ScreenConfig = ScreenConfig())
  case class ScreenConfig(
      width: Int = 1920,
      height: Int = 1080,
      depth: Int = 24
  )
}
