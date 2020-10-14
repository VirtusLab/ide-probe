package org.virtuslab.ideprobe.protocol

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AwaitIdle(initialWait: FiniteDuration, newTaskWait: FiniteDuration, checkFrequency: FiniteDuration)

object AwaitIdle {
  val Default: AwaitIdle = AwaitIdle(5.seconds, 2.seconds, 50.millis)
}