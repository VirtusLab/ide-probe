package org.virtuslab.ideprobe.scala

import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.scala.handlers.{ProjectImport, SbtSettings, ScalaTestRunConfiguration}
import org.virtuslab.ideprobe.scala.protocol.ScalaEndpoints
import org.virtuslab.ideprobe.{IdeProbeService, ProbeHandlerContributor}
import scala.concurrent.ExecutionContext

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
