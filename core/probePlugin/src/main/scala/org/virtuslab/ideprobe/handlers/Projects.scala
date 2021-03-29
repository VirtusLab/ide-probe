package org.virtuslab.ideprobe.handlers

import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.roots.{CompilerProjectExtension, ProjectRootManager}
import com.intellij.projectImport.ProjectImportProvider
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.ProbePluginExtensions._
import org.virtuslab.ideprobe.protocol
import org.virtuslab.ideprobe.protocol.{ProjectRef, Sdk}
import java.nio.file.Path
import scala.annotation.tailrec

object Projects extends IntelliJApi {

  def resolve(ref: ProjectRef): Project = {
    ref match {
      case ProjectRef.Default      => current
      case ProjectRef.ByName(name) => findProject(name)
    }
  }

  def current: Project = {
    allOpenProjects match {
      case Nil          => error("No open projects")
      case List(single) => single
      case multiple     => error(s"Ambiguous Projects.current call, multiple projects open: $multiple")
    }
  }

  def all: Seq[ProjectRef] = {
    allOpenProjects.map(toRef)
  }

  def open(path: Path): Unit = {
    val project = ProjectUtil.openOrImport(path, null, false)
    if (project == null) error(s"Could not load project '$path'")
  }

  def importFromSources(path: Path, handleStep: PartialFunction[Any, Unit]): Unit = {
    val wizard = {
      val providers = ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions.filter(_.canCreateNewProject)
      val wizard = runOnUISync {
        write {
          ImportModuleAction.createImportWizard(null, null, VFS.toVirtualFile(path), providers: _*)
        }
      }

      @tailrec def goThroughWizard(): Unit = {
        val step = wizard.getCurrentStepObject
        handleStep.applyOrElse(step, (_: Any) => ())
        runOnUISync(step.updateDataModel())
        if (wizard.isLast) {
          runOnUISync(wizard.doFinishAction())
        } else {
          runOnUISync(wizard.doNextAction())
          if (step == wizard.getCurrentStepObject) {
            error(s"Failed to proceed in project import wizard step $step")
          }
          goThroughWizard()
        }
      }

      goThroughWizard()
      wizard
    }

    runOnUISync(ImportModuleAction.createFromWizard(null, wizard))
  }

  def close(ref: ProjectRef): Unit = {
    val project = resolve(ref)
    runOnUISync {
      ProjectManagerEx.getInstanceEx.closeAndDispose(project)
    }
  }

  def model(ref: ProjectRef): protocol.Project = read {
    val project = resolve(ref)
    val modules = ModuleManager.getInstance(project).getSortedModules
    val mappedModules = modules.map { module =>
      val dependencies = Modules.dependencies(module).map(_.toRef).toSet
      val contentRoots = Modules.contentRoots(module)
      protocol.Module(module.getName, contentRoots, dependencies, Option(module.getModuleTypeName))
    }

    protocol.Project(project.getName, project.getBasePath, mappedModules)
  }

  def sdk(ref: ProjectRef): Option[Sdk] = read {
    val project = resolve(ref)
    val sdk = ProjectRootManager.getInstance(project).getProjectSdk
    Option(sdk).map(Sdks.convert)
  }

  private def findProject(name: String): Project = {
    val projects = allOpenProjects
    projects
      .find(p => p.getName == name)
      .getOrElse {
        val openProjects =
          if (projects.isEmpty) "There are no open projects"
          else s"Currently open projects are: ${projects.map(_.getName).mkString(", ")}"

        error(s"Could not find project $name. $openProjects")
      }
  }

  def setCompilerOutput(projectRef: ProjectRef, path: Path): Unit = {
    val project = Projects.resolve(projectRef)
    val compilerProjectExtension = CompilerProjectExtension.getInstance(project)
    compilerProjectExtension.setCompilerOutputUrl(path.toUri.toString)
  }

  private def allOpenProjects = {
    ProjectManager.getInstance.getOpenProjects.toList
  }

  private def toRef(project: Project): ProjectRef = ProjectRef(project.getName)
}
