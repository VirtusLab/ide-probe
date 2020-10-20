package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.protocol.AwaitIdleParams

case class EndpointsConfig(awaitIdleParams: AwaitIdleParams)

object EndpointsConfig {
  def apply(): EndpointsConfig = EndpointsConfig(awaitIdleParams = AwaitIdleParams.Default)
}
