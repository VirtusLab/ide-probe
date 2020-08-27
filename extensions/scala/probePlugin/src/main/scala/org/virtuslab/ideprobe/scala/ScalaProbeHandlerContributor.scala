package org.virtuslab.ideprobe.scala

import org.virtuslab.ideprobe.ProbeHandlerContributor
import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.scala.handlers.SbtSettings
import org.virtuslab.ideprobe.scala.protocol.ScalaEndpoints

class ScalaProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(ScalaEndpoints.ChangeSbtProjectSettings)((SbtSettings.changeProjectSettings _).tupled)
      .on(ScalaEndpoints.GetSbtProjectSettings)(SbtSettings.getProjectSettings)
  }
}
