package org.virtuslab.ideprobe.protocol

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AwaitIdleParams(
    initialWait: FiniteDuration = 5.seconds,
    newTaskWait: FiniteDuration = 2.seconds,
    checkFrequency: FiniteDuration = 50.millis,
    active: Boolean = true
)

object AwaitIdleParams {
  val Default = AwaitIdleParams()
}
