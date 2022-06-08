package org.virtuslab.ideprobe.scala.handlers

import com.intellij.projectImport.{ImportChooserStep, ProjectImportProvider}
import com.intellij.ui.components.{JBList, JBRadioButton}
import java.nio.file.Path
import org.virtuslab.ideprobe.handlers.{IntelliJApi, Projects}

import java.lang.reflect.{Field, Method}
import scala.annotation.tailrec

object ProjectImport extends IntelliJApi {


  @tailrec
  final def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
    try cl.getDeclaredMethod(name, parameters: _*)
    catch {
      case e: NoSuchMethodException =>
        if (cl.getSuperclass == null) throw e
        else getMethod(cl.getSuperclass, name, parameters: _*)
    }
  }


  @tailrec
  final def getField(cl: Class[_], name: String): Field = {
    try cl.getField(name)
    catch {
      case e: NoSuchFieldException =>
        if (cl.getSuperclass == null) throw e
        else getField(cl.getSuperclass, name)
    }
  }

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

    val bspProjectImportProvideClass = this.getClass.getClassLoader.loadClass("org.jetbrains.bsp.project.importing.BspProjectImportProvider")
    val providersList = step.field[JBList[ProjectImportProvider]]("list")
    val bsp = providersList.items
      .collectFirst { case p if p.getClass == bspProjectImportProvideClass => p }
      .getOrElse(error(s"Could not find bsp import provider. Available providers are ${providersList.items}"))
    providersList.setSelectedValue(bsp, false)
  }
}
