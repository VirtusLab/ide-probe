package org.virtuslab.intellij.scala.probe

import org.virtuslab.ProbeHandlerContributor
import org.virtuslab.ProbeHandlers.ProbeHandler
import org.virtuslab.intellij.scala.probe.handlers.SbtSettings
import org.virtuslab.intellij.scala.protocol.SbtEndpoints

class SbtProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(SbtEndpoints.ChangeSbtProjectSettings)((SbtSettings.changeProjectSettings _).tupled)
      .on(SbtEndpoints.GetSbtProjectSettings)(SbtSettings.getProjectSettings)
  }
}
