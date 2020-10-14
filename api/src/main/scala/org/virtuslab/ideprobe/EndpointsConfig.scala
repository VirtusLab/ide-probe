package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.protocol.AwaitIdle

case class EndpointsConfig(awaitIdle: AwaitIdle)

object EndpointsConfig {
  def apply(): EndpointsConfig = EndpointsConfig(awaitIdle = AwaitIdle.Default)
}
