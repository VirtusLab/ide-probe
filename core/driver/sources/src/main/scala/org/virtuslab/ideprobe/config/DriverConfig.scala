package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.DriverConfig.LaunchParameters

import scala.concurrent.duration._

case class DriverConfig(
    launch: LaunchParameters = LaunchParameters(),
    check: CheckConfig = CheckConfig(),
    headless: Boolean = false,
    errorTexts: Seq[String] = Nil,
    vmOptions: Seq[String] = Nil,
    env: Map[String, String] = Map.empty
)

object DriverConfig {
  case class LaunchParameters(
      command: Seq[String] = Nil,
      timeout: FiniteDuration = 30.seconds
  )
}
