package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.DriverConfig.{LaunchParameters, ScreenConfig}

import scala.concurrent.duration._

case class DriverConfig(
    launch: LaunchParameters = LaunchParameters(),
    check: CheckConfig = CheckConfig(),
    screen: ScreenConfig = ScreenConfig(),
    headless: Boolean = false,
    vmOptions: Seq[String] = Nil,
    env: Map[String, String] = Map.empty
)

object DriverConfig {
  case class LaunchParameters(
      command: Seq[String] = Nil,
      timeout: FiniteDuration = 30.seconds
  )
  case class ScreenConfig(
      width: Int = 1920,
      height: Int = 1080,
      depth: Int = 24
  )
}
