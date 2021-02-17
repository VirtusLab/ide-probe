package com.twitter.intellij.pants

import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension

trait OpenProjectFixture extends PantsFixture with BspFixture { this: RobotPluginExtension =>

  def openProjectWithBsp(intelliJ: RunningIntelliJFixture): ProjectRef = {
    val projectPath =
      runFastpassCreate(intelliJ.config, intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.withRobot.openProject(projectPath)
  }

  def openProjectWithPants(intelliJ: RunningIntelliJFixture): ProjectRef = {
    val projectPath = runPantsIdeaPlugin(intelliJ.workspace, targetsFromConfig(intelliJ))
    intelliJ.probe.withRobot.openProject(projectPath)
  }

  private def targetsFromConfig(intelliJ: RunningIntelliJFixture): Seq[String] = {
    intelliJ.config[Seq[String]]("pants.import.targets")
  }
}
