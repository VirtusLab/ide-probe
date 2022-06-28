package org.virtuslab.ideprobe.scala

import java.nio.file.Path

import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.WaitLogic
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.TestsRunResult
import org.virtuslab.ideprobe.scala.protocol._

object ScalaProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.scala"

  def apply(driver: ProbeDriver): ScalaProbeDriver = driver.as(pluginId, new ScalaProbeDriver(_))
}

final class ScalaProbeDriver(val driver: ProbeDriver) extends AnyVal {
  def getSbtProjectSettings(project: ProjectRef = ProjectRef.Default): SbtProjectSettings = {
    driver.send(ScalaEndpoints.GetSbtProjectSettings, project)
  }

  def setSbtProjectSettings(
      settings: SbtProjectSettingsChangeRequest,
      project: ProjectRef = ProjectRef.Default,
      waitLogic: WaitLogic = WaitLogic.Default
  ): Unit = driver.withAwait(waitLogic) {
    driver.send(ScalaEndpoints.ChangeSbtProjectSettings, (project, settings))
  }

  def importBspProject(path: Path, waitLogic: WaitLogic = WaitLogic.Default): ProjectRef = {
    driver.awaitForProjectOpen(waitLogic) {
      driver.send(ScalaEndpoints.ImportBspProject, path)
    }
  }

  /**
   * Runs the specified ScalaTest configuration
   */
  def run(runConfiguration: ScalaTestRunConfiguration): TestsRunResult =
    driver.send(ScalaEndpoints.RunScalaTest, runConfiguration)
}
