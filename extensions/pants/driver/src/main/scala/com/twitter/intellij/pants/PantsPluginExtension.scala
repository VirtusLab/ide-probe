package com.twitter.intellij.pants

import org.virtuslab.ideprobe.dependencies.{DependencyProvider, Plugin}
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.{IdeProbeFixture, ProbeDriver}
import org.virtuslab.ideprobe.BuildInfo
import scala.language.implicitConversions

trait PantsPluginExtension extends OpenProjectFixture with RobotPluginExtension {
  this: IdeProbeFixture =>
  val pantsProbePlugin: Plugin = Plugin.Bundled(s"ideprobe-pants-${BuildInfo.version}.zip")

  DependencyProvider.registerBuilder(PantsPluginBuilder)

  registerFixtureTransformer(_.withPlugin(pantsProbePlugin))
  registerFixtureTransformer(_.withAfterWorkspaceSetup(PantsSetup.overridePantsVersion))

  implicit def pantsProbeDriver(driver: ProbeDriver): PantsProbeDriver = PantsProbeDriver(driver)

}
