package org.virtuslab.ideprobe.scala

import java.nio.file.Path

import com.intellij.projectImport.{ImportChooserStep, ProjectImportProvider}
import com.intellij.ui.components.{JBList, JBRadioButton}
import org.jetbrains.bsp.project.importing.BspProjectImportProvider
import org.virtuslab.ideprobe.ProbeHandlerContributor
import org.virtuslab.ideprobe.ProbeHandlers.ProbeHandler
import org.virtuslab.ideprobe.handlers.App.{JListOps, ReflectionOps}
import org.virtuslab.ideprobe.handlers.Projects
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.scala.handlers.SbtSettings
import org.virtuslab.ideprobe.scala.protocol.ScalaEndpoints

class ScalaProbeHandlerContributor extends ProbeHandlerContributor {
  override def registerHandlers(handler: ProbeHandler): ProbeHandler = {
    handler
      .on(ScalaEndpoints.ChangeSbtProjectSettings)((SbtSettings.changeProjectSettings _).tupled)
      .on(ScalaEndpoints.GetSbtProjectSettings)(SbtSettings.getProjectSettings)
      .on(ScalaEndpoints.ImportBspProject)(importBspProject)
  }

  def importBspProject(path: Path): ProjectRef = {
    Projects.importFromSources(
      path, {
        case step: ImportChooserStep =>
          selectBspImportModel(step)
      }
    )
  }

  private def selectBspImportModel(step: ImportChooserStep): Unit = {
    val importCheckbox = step.field[JBRadioButton]("importFrom")
    importCheckbox.setSelected(true)

    val providersList = step.field[JBList[ProjectImportProvider]]("list")
    val pants = providersList.items
      .collectFirst { case p: BspProjectImportProvider => p }
      .getOrElse(throw new RuntimeException(s"Could not find pants import provider. Available providers are ${providersList.items}"))
    providersList.setSelectedValue(pants, false)
  }

}
