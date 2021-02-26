package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.protocol.AwaitIdleParams

case class EndpointsConfig(awaitIdle: AwaitIdleParams)

object EndpointsConfig {
  def apply(): EndpointsConfig = EndpointsConfig(awaitIdle = AwaitIdleParams.Default)
}
