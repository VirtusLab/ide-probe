package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.{RunningIntelliJFixture, WaitLogic}

trait BazelOpenProjectFixture extends BazeliskExtension { this: RobotPluginExtension =>

  def openProjectWithBazel(
      intelliJ: RunningIntelliJFixture,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    val importSpec = intelliJ.config[BazelImportSpec]("bazel.import")
    val robotDriver = intelliJ.probe.withRobot
    BazelProbeDriver(intelliJ.probe)
      .importProject(importSpec, intelliJ.workspace, robotDriver.extendWaitLogic(waitLogic))
  }

}
