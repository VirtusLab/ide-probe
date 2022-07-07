package org.virtuslab.ideprobe.scala

import scala.concurrent.ExecutionContext

import org.virtuslab.ideprobe.IdeProbeService
import org.virtuslab.ideprobe.ProbeHandlerContributor
import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.scala.handlers.ProjectImport
import org.virtuslab.ideprobe.scala.handlers.SbtSettings
import org.virtuslab.ideprobe.scala.handlers.ScalaTestRunConfiguration
import org.virtuslab.ideprobe.scala.protocol.ScalaEndpoints

class ScalaProbeHandlerContributor extends ProbeHandlerContributor {
  private implicit val ec: ExecutionContext = IdeProbeService.executionContext

  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(ScalaEndpoints.ChangeSbtProjectSettings)((SbtSettings.changeProjectSettings _).tupled)
      .on(ScalaEndpoints.GetSbtProjectSettings)(SbtSettings.getProjectSettings)
      .on(ScalaEndpoints.RunScalaTest)(ScalaTestRunConfiguration.execute)
      .on(ScalaEndpoints.ImportBspProject)(ProjectImport.importBspProject)
  }

}
