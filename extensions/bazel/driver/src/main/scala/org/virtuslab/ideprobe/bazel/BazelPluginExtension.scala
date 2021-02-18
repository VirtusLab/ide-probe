package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.dependencies.Plugin
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.{BuildInfo, IdeProbeFixture, ProbeDriver, error}
import scala.language.implicitConversions

trait BazelPluginExtension extends BazelOpenProjectFixture with RobotPluginExtension { this: IdeProbeFixture =>

  if (!System.getProperty("java.version").startsWith("11.")) {
    error("Bazel tests must run on java 11. Also make sure JAVA_HOME points to java 11")
  }

  val bazelProbePlugin: Plugin = Plugin.Bundled(s"ideprobe-bazel-${BuildInfo.version}.zip")

  registerFixtureTransformer(_.withPlugin(bazelProbePlugin))

  registerFixtureTransformer(_.withAfterWorkspaceSetup { (intelliJ, ws) =>
    installBazelisk(bazelPath(ws), intelliJ.config)
  })

  implicit def bazelProbeDriver(driver: ProbeDriver): BazelProbeDriver = new BazelProbeDriver(driver)

}
