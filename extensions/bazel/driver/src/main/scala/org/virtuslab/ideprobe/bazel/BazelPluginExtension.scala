package org.virtuslab.ideprobe.bazel

import org.virtuslab.ideprobe.Extensions.PathExtension
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.{IdeProbeFixture, OS, ProbeDriver, Shell, error}
import org.virtuslab.ideprobe.dependencies.InternalPlugins

import java.nio.file.Paths
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
    // Hack for disabling setsid call with unsupported --wait on older CentOS
    if (OS.Current == OS.Unix && Shell.run("/usr/bin/setsid", "--wait", "ls").isFailed) {
        Paths.get(sys.props("user.home"), ".intellij-experiments").write("blaze.command.process.group=false")
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
