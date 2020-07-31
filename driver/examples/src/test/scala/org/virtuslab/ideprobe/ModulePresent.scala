package org.virtuslab.ideprobe

import org.junit.{Assert, Test}

class ModulePresent extends ModuleTest {

  def testPresentModules(intelliJ: RunningIntelliJFixture): Unit = {
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)
    val projectModules = project.modules.map(_.name)
    val modulesFromConfig = intelliJ.test.modules

    modulesFromConfig.foreach { module =>
      Assert.assertTrue(s"Module $module does not exist", projectModules contains module)
    }
  }

  @Test
  def run: Unit = fixtures.foreach { fixture =>
    fixture.run(testPresentModules)
  }
}
