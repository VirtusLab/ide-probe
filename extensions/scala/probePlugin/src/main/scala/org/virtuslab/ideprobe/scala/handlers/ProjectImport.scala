package org.virtuslab.ideprobe.scala.handlers

import java.nio.file.Path

import com.intellij.projectImport.ImportChooserStep
import com.intellij.projectImport.ProjectImportProvider
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBRadioButton
import org.jetbrains.bsp.project.importing.BspProjectImportProvider

import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.handlers.Projects

object ProjectImport extends IntelliJApi {
  def importBspProject(path: Path): Unit = {
    Projects.importFromSources(
      path,
      { case step: ImportChooserStep =>
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
