package org.virtuslab.ideprobe.scala.handlers

import com.intellij.projectImport.{ImportChooserStep, ProjectImportProvider}
import com.intellij.ui.components.{JBList, JBRadioButton}
import java.nio.file.Path
import org.jetbrains.bsp.project.importing.BspProjectImportProvider
import org.virtuslab.ideprobe.handlers.{IntelliJApi, Projects}
import org.virtuslab.ideprobe.protocol.ProjectRef

object ProjectImport extends IntelliJApi {
  def importBspProject(path: Path): Unit = {
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
    val bsp = providersList.items
      .collectFirst { case p: BspProjectImportProvider => p }
      .getOrElse(error(s"Could not find bsp import provider. Available providers are ${providersList.items}"))
    providersList.setSelectedValue(bsp, false)
  }
}
