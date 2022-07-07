package org.virtuslab.ideprobe.pants

import org.virtuslab.ideprobe.ProbeHandlerContributor
import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.pants.handlers.PantsImport
import org.virtuslab.ideprobe.pants.handlers.PantsSettings
import org.virtuslab.ideprobe.pants.handlers.PythonProject
import org.virtuslab.ideprobe.pants.protocol.PantsEndpoints

class PantsProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(PantsEndpoints.ImportPantsProject)((PantsImport.importProject _).tupled)
      .on(PantsEndpoints.ChangePantsProjectSettings)((PantsSettings.changeProjectSettings _).tupled)
      .on(PantsEndpoints.GetPantsProjectSettings)(PantsSettings.getProjectSettings)
      .on(PantsEndpoints.GetPythonFacets)(PythonProject.facets)
  }
}
