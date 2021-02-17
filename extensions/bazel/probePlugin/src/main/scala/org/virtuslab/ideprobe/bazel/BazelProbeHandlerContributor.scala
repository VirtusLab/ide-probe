package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.ProbeHandlerContributor
import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.bazel.handlers.Settings
import org.virtuslab.ideprobe.bazel.protocol.BazelEndpoints

class BazelProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(BazelEndpoints.SetupBazelExecutable)(Settings.setupBazelExecutable)
  }

}
