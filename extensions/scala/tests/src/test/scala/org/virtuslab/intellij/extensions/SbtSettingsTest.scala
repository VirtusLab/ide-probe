package org.virtuslab.intellij.extensions

import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.protocol.Setting
import org.virtuslab.intellij.scala.SbtTestSuite
import org.virtuslab.intellij.scala.protocol.{SbtProjectSettings, SbtProjectSettingsChangeRequest}


class SbtSettingsTest extends SbtTestSuite {
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
    val expectedSettings = SbtProjectSettings(useSbtShellForImport = true, useSbtShellForBuild = true, allowSbtVersionOverride = false)
    val actualSettings = intelliJ.probe.getSbtProjectSettings()

    Assert.assertTrue("Settings are not changed correctly", actualSettings == expectedSettings)
  }
}

