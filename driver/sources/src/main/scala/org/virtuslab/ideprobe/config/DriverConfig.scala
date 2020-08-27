package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.config.DriverConfig.LaunchParameters

import scala.concurrent.duration._

case class DriverConfig(
    launch: LaunchParameters = LaunchParameters(),
    check: CheckConfig = CheckConfig(),
    headless: Boolean = false,
    vmOptions: Seq[String] = Nil
)

object DriverConfig {
  case class LaunchParameters(
    command: Seq[String] = Nil,
    timeout: FiniteDuration = 30.seconds
  )
}
