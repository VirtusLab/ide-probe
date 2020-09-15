package org.virtuslab.intellij.extensions

import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Config

final class BspImportTest extends ScalaPluginTestSuite {

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")

  @Test
  def importSbtProject(): Unit = {
    fixtureFromConfig(config).run { intellij =>
      val projectRef = intellij.probe.importBspProject(intellij.workspace.resolve("root"))
      val project = intellij.probe.projectModel(projectRef)
      val modules = project.modules.map(_.name).toSet

      Assert.assertEquals(3, modules.size)
    }
  }

}