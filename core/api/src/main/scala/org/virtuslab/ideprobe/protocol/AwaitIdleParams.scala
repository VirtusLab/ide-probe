package org.virtuslab.ideprobe.protocol

import org.virtuslab.ideprobe.ConfigFormat
import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert
import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AwaitIdleParams(
    initialWait: FiniteDuration = 5.seconds,
    newTaskWait: FiniteDuration = 2.seconds,
    checkFrequency: FiniteDuration = 50.millis,
    active: Boolean = true
)

object AwaitIdleParams extends ConfigFormat {
  implicit val format: ConfigConvert[AwaitIdleParams] = deriveConvert[AwaitIdleParams]

  val Default: AwaitIdleParams = AwaitIdleParams()
}
