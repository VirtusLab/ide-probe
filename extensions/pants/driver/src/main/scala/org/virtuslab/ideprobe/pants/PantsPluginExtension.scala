package org.virtuslab.ideprobe.pants

import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.dependencies.DependencyProvider
import org.virtuslab.ideprobe.dependencies.InternalPlugins
import org.virtuslab.ideprobe.robot.RobotPluginExtension

import scala.language.implicitConversions

trait PantsPluginExtension extends PantsOpenProjectFixture with RobotPluginExtension {
  this: IdeProbeFixture =>

  DependencyProvider.registerBuilder(PantsPluginBuilder)

  registerFixtureTransformer(InternalPlugins.installCrossVersionPlugin("ideprobe-pants"))
  registerFixtureTransformer(_.withAfterWorkspaceSetup(PantsSetup.overridePantsVersion))

  implicit def pantsProbeDriver(driver: ProbeDriver): PantsProbeDriver = PantsProbeDriver(driver)

}
