package org.virtuslab.ideprobe.robot

import java.net.URI
import org.virtuslab.ideprobe.{IdeProbeFixture, Network, ProbeDriver}
import org.virtuslab.ideprobe.dependencies.Plugin

trait RobotPluginExtension extends RobotSyntax { this: IdeProbeFixture =>
  def robotPlugin: Plugin = {
    val repository = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies"
    val group = "org.jetbrains.test".replace(".", "/")
    val artifact = "robot-server-plugin"
    val version = BuildInfo.robotVersion
    val uri = s"$repository/$group/$artifact/$version/$artifact-$version.zip"
    Plugin.Direct(new URI(uri))
  }

  registerFixtureTransformer(_.withPlugin(robotPlugin))
  registerFixtureTransformer(_.withVmOptions(s"-D${RobotProbeDriver.robotPortProperty}=${Network.freePort()}"))

  implicit class AsRobotDriver(driver: ProbeDriver) {
    def withRobot: RobotProbeDriver = RobotProbeDriver(driver)
  }
}
