package org.virtuslab.intellij.scala

import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.intellij.scala.protocol.{SbtEndpoints, SbtProjectSettings, SbtProjectSettingsChangeRequest}

object SbtProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.scalaplugin"

  def apply(driver: ProbeDriver): SbtProbeDriver = driver.as(pluginId, new SbtProbeDriver(_))
}

final class SbtProbeDriver(val driver: ProbeDriver) extends AnyVal {
  def getSbtProjectSettings(project: ProjectRef = ProjectRef.Default): SbtProjectSettings = {
    driver.send(SbtEndpoints.GetSbtProjectSettings, project)
  }

  def setSbtProjectSettings(
                             settings: SbtProjectSettingsChangeRequest,
                             project: ProjectRef = ProjectRef.Default
                           ): Unit = {
    driver.send(SbtEndpoints.ChangeSbtProjectSettings, (project, settings))
  }
}
