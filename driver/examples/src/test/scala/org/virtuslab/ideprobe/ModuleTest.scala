package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.virtuslab.ideprobe.protocol.{JUnitRunConfiguration, ModuleRef}

import scala.concurrent.ExecutionContext

class ModuleTest {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def fixtureFromConfig(configName: String): IntelliJFixture =
    IntelliJFixture.fromConfig(Config.fromClasspath(configName))

  @ParameterizedTest
  @ValueSource(
    strings = Array("projects/shapeless/ideprobe.conf", "projects/cats/ideprobe.conf", "projects/dokka/ideprobe.conf")
  )
  def verifyModulesPresent(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)
    val projectModules = project.modules.map(_.name)
    val modulesFromConfig = intelliJ.config.get[Seq[String]]("test.modules").get

    val missingModules = modulesFromConfig.diff(projectModules)
    Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
  }

  @ParameterizedTest
  @ValueSource(strings = Array("projects/shapeless/ideprobe.conf", "projects/cats/ideprobe.conf"))
  def runTestsInModules(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)
    val modulesFromConfig = intelliJ.config.get[Seq[String]]("test.modules").get
    val moduleRefs = modulesFromConfig.map(ModuleRef(_, project.name))
    val runConfigs = moduleRefs.map(JUnitRunConfiguration(_, None, None, None, None))

    runConfigs.map(intelliJ.probe.run).foreach { result =>
      Assert.assertTrue("Test failed", result.isSuccess)
    }
  }
}
