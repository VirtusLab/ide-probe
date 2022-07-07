package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.RunningIntelliJFixture
import org.virtuslab.ideprobe.WaitLogic
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.robot.RobotPluginExtension

import org.virtuslab.ideprobe.Extensions.PathExtension

trait BazelOpenProjectFixture extends BazeliskExtension { this: RobotPluginExtension =>

  def openProjectWithBazel(
      intelliJ: RunningIntelliJFixture,
      waitLogic: WaitLogic = WaitLogic.Default
  ): ProjectRef = {
    val importSpec = intelliJ.config[BazelImportSpec]("bazel.import")

    importSpec.directories.foreach { dirName =>
      val dir = intelliJ.workspace.resolve(dirName)
      assert(dir.isDirectory, s"Not a directory: $dir")
    }

    val robotDriver = intelliJ.probe.withRobot
    BazelProbeDriver(intelliJ.probe)
      .importProject(importSpec, intelliJ.workspace, robotDriver.extendWaitLogic(waitLogic))
  }

}
