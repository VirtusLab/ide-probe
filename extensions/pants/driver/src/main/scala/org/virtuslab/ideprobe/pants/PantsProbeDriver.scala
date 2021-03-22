package org.virtuslab.ideprobe.pants

import java.nio.file.Path
import org.virtuslab.ideprobe.{ProbeDriver, WaitLogic}
import org.virtuslab.ideprobe.pants.protocol.{
  PantsEndpoints,
  PantsProjectSettings,
  PantsProjectSettingsChangeRequest,
  PythonFacet
}
import org.virtuslab.ideprobe.protocol.{IdeNotification, ModuleRef, ProjectRef}
import org.virtuslab.ideprobe.robot.RobotProbeDriver
import org.virtuslab.ideprobe.robot.RobotSyntax._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object PantsProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.pants"

  def apply(driver: ProbeDriver): PantsProbeDriver = driver.as(pluginId, new PantsProbeDriver(_))
}

final class PantsProbeDriver(val driver: ProbeDriver) extends AnyVal {
  def getPythonFacets(moduleRef: ModuleRef): Seq[PythonFacet] = {
    driver.send(PantsEndpoints.GetPythonFacets, moduleRef)
  }

  def importProject(
      path: Path,
      settings: PantsProjectSettingsChangeRequest,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    driver.awaitForProjectOpen(waitLogic) {
      driver.send(PantsEndpoints.ImportPantsProject, (path, settings))
    }
  }

  def getPantsProjectSettings(project: ProjectRef = ProjectRef.Default): PantsProjectSettings = {
    driver.send(PantsEndpoints.GetPantsProjectSettings, project)
  }

  def setPantsProjectSettings(
      settings: PantsProjectSettingsChangeRequest,
      project: ProjectRef = ProjectRef.Default,
      waitLogic: WaitLogic = WaitLogic.Default
  ): Unit = driver.withAwait(waitLogic) {
    driver.send(PantsEndpoints.ChangePantsProjectSettings, (project, settings))
  }

  def compileAllTargets(timeout: Duration = 10.minutes): PantsBuildResult = {
    driver.invokeActionAsync("com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction")
    val compiledNotification = Try(driver.awaitNotification("Compile message", timeout))
    val robot = RobotProbeDriver(driver).robot

    val output = (for {
      panel <- robot.findOpt(query.className("PantsConsoleViewPanel"))
      editor <- panel.findOpt(query.className("EditorComponentImpl"))
    } yield editor.fullText).getOrElse("<output not found>")

    compiledNotification match {
      case Success(notification) if notification.severity == IdeNotification.Severity.Info =>
        PantsBuildResult(PantsBuildResult.Status.Passed, output)
      case Success(_) =>
        PantsBuildResult(PantsBuildResult.Status.Failed, output)
      case Failure(exception) =>
        exception.printStackTrace()
        PantsBuildResult(PantsBuildResult.Status.Timeout, output)
    }
  }
}
