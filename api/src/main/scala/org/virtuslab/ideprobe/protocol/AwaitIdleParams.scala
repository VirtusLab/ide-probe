package org.virtuslab.ideprobe.protocol

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AwaitIdleParams(initialWait: FiniteDuration, newTaskWait: FiniteDuration, checkFrequency: FiniteDuration)

object AwaitIdleParams {
  val Default: AwaitIdleParams = AwaitIdleParams(5.seconds, 2.seconds, 50.millis)
}