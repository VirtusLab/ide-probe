package org.virtuslab.intellij.extensions

import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.dependencies.DependencyProvider
import org.virtuslab.ideprobe.protocol.Setting
import org.virtuslab.ideprobe.scala.ScalaTestSuite
import org.virtuslab.ideprobe.scala.protocol.{SbtProjectSettings, SbtProjectSettingsChangeRequest}

class SbtSettingsTest extends ScalaTestSuite {
  DependencyProvider.registerBuilder(ScalaPluginBuilder)

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def setSbtSettings: Unit = fixtureFromConfig(config).run { intelliJ =>
    intelliJ.probe.openProject(intelliJ.workspace.resolve("root"))
    intelliJ.probe.setSbtProjectSettings(
      SbtProjectSettingsChangeRequest(
        useSbtShellForImport = Setting.Changed(true),
        useSbtShellForBuild = Setting.Changed(true),
        allowSbtVersionOverride = Setting.Changed(false)
      )
    )
    val expectedSettings =
      SbtProjectSettings(useSbtShellForImport = true, useSbtShellForBuild = true, allowSbtVersionOverride = false)
    val actualSettings = intelliJ.probe.getSbtProjectSettings()

    Assert.assertEquals("Settings are not changed correctly", expectedSettings, actualSettings)
  }
}
