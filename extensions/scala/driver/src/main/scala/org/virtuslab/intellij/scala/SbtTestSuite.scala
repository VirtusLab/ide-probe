package org.virtuslab.intellij.scala

import java.nio.file.Files

import org.virtuslab.ideprobe.{Config, IntegrationTestSuite, IntelliJFixture, ProbeDriver, RunningIntelliJFixture}
import org.virtuslab.ideprobe.dependencies.{InternalPlugins, Plugin}
import org.virtuslab.ideprobe.Extensions._

class SbtTestSuite extends IntegrationTestSuite {
  /**
   * The presence of .idea can prevent automatic import of gradle project
   */
  protected def deleteIdeaSettings(intelliJ: RunningIntelliJFixture) = {
    val path = intelliJ.workspace.resolve(".idea")
    if (Files.exists(path)) path.delete()
  }

  protected def fixtureFromConfig(configName: String): IntelliJFixture =
    transformFixture(IntelliJFixture.fromConfig(Config.fromClasspath(configName)))

  val scalaProbePlugin: Plugin = InternalPlugins.bundle("ideprobe-scala")

  override protected def transformFixture(fixture: IntelliJFixture): IntelliJFixture = {
    fixture
      .withPlugin(scalaProbePlugin)
      .withAfterIntelliJInstall { (_, inteliJ) =>
        inteliJ.paths.plugins.resolve("ideprobe/lib/scala-library.jar").delete()
      }
  }

  implicit def sbtProbeDriver(driver: ProbeDriver): SbtProbeDriver = SbtProbeDriver(driver)
}
