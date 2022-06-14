package org.virtuslab.ideprobe.scala

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}

@RunWith(classOf[Parameterized])
final class BspImportTest(val scalaPlugin: Plugin.Versioned, val intellijVersion: IntelliJVersion)
  extends ScalaPluginTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    fixtureFromConfig(config)
      .withVersion(intellijVersion)
      .withPlugin(scalaPlugin)
      .run { intellij =>
      val projectRef = intellij.probe.importBspProject(intellij.workspace.resolve("root"))
      val project = intellij.probe.projectModel(projectRef)
      val modules = project.modules.map(_.name).toSet

      Assert.assertEquals(4, modules.size)
    }
  }

}

object BspImportTest extends ProbeDriverTestParamsProvider