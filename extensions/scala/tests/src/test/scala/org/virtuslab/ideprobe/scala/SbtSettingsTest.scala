package org.virtuslab.ideprobe.scala

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}
import org.virtuslab.ideprobe.protocol.Setting
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettings
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

@RunWith(classOf[Parameterized])
class SbtSettingsTest(val scalaPlugin: Plugin.Versioned, val intellijVersion: IntelliJVersion)
  extends ScalaPluginTestSuite {

  @Test
  def setSbtSettings(): Unit = {
    fixtureFromConfig("SbtProject/ideprobe.conf").run { intelliJ =>
      intelliJ.probe.withRobot.openProject(intelliJ.workspace.resolve("root"))

      intelliJ.probe.setSbtProjectSettings(
        SbtProjectSettingsChangeRequest(
          useSbtShellForImport = Setting.Changed(true),
          useSbtShellForBuild = Setting.Changed(true),
          allowSbtVersionOverride = Setting.Changed(false)
        )
      )

      val expectedSettings =
        SbtProjectSettings(
          useSbtShellForImport = true,
          useSbtShellForBuild = true,
          allowSbtVersionOverride = false
        )

      val actualSettings = intelliJ.probe.getSbtProjectSettings()

      Assert.assertEquals("Settings are not changed correctly", expectedSettings, actualSettings)
    }
  }
}

object SbtSettingsTest extends ProbeDriverTestParamsProvider