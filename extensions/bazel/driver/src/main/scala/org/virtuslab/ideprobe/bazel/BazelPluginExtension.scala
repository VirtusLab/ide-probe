package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.robot.{RobotPluginExtension, RobotProbeDriver}
import org.virtuslab.ideprobe.{IdeProbeFixture, ProbeDriver, error}
import org.virtuslab.ideprobe.dependencies.InternalPlugins

import scala.language.implicitConversions

trait BazelPluginExtension extends BazelOpenProjectFixture with RobotPluginExtension {
  this: IdeProbeFixture =>

  {
    val javaVersion = System.getProperty("java.version")
    if (!javaVersion.startsWith("11.")) {
      error(
        "Bazel tests must run on java 11. Also make sure JAVA_HOME points to java 11. " +
          s"Current java version: $javaVersion and JAVA_HOME: ${sys.env.get("JAVA_VERSION")}"
      )
    }
  }
  registerFixtureTransformer(InternalPlugins.installCrossVersionPlugin("ideprobe-bazel"))

  registerFixtureTransformer(_.withAfterWorkspaceSetup { (intelliJ, ws) =>
    installBazelisk(bazelPath(ws), intelliJ.config)
  })

  registerFixtureTransformer(_.withAfterIntelliJStartup { (_, intelliJ) =>
    intelliJ.probe.setupBazelExec(bazelPath(intelliJ.workspace))
  })

  implicit def bazelProbeDriver(driver: ProbeDriver): BazelProbeDriver =
    BazelProbeDriver(driver)

}
