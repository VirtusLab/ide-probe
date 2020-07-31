package org.virtuslab.ideprobe

import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.protocol.{JUnitRunConfiguration, ModuleRef}

class ModuleTestRun extends ModuleTest {

  def runAllModuleTests(intelliJ: RunningIntelliJFixture): Unit = {
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)
    val moduleRefs = intelliJ.test.modules.map(ModuleRef(_, project.name))
    val runConfigs = moduleRefs.map(JUnitRunConfiguration(_, None, None, None, None))

    runConfigs.map(intelliJ.probe.run).foreach { result =>
      Assert.assertTrue("Test failed", result.isSuccess)
    }
  }

  @Test
  def run: Unit = fixtures.foreach { fixture =>
    fixture.run(runAllModuleTests)
  }
}
