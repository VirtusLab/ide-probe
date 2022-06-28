package org.virtuslab.ideprobe.pants

import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.WaitLogic
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension

trait PantsOpenProjectFixture extends PantsFixture with BspFixture { this: RobotPluginExtension =>

  def openProjectWithBsp(
      intelliJ: RunningIntelliJFixture,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    val projectPath =
      runFastpassCreate(intelliJ.config, intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.withRobot.openProject(projectPath, waitLogic)
  }

  def openProjectWithPants(
      intelliJ: RunningIntelliJFixture,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    val projectPath = runPantsIdeaPlugin(intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.withRobot.openProject(projectPath, waitLogic)
  }

  private def targetsFromConfig(intelliJ: RunningIntelliJFixture): Seq[String] = {
    intelliJ.config[Seq[String]]("pants.import.targets")
  }
}
