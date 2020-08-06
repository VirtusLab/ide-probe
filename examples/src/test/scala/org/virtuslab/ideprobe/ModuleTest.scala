package org.virtuslab.ideprobe

import java.nio.file.Files
import java.util.concurrent.Executors

import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.{JUnitRunConfiguration, ModuleRef}

import scala.concurrent.ExecutionContext

class ModuleTest extends RobotExtensions {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  private def fixtureFromConfig(configName: String): IntelliJFixture =
    IntelliJFixture.fromConfig(Config.fromClasspath(configName))

  /**
   * The presence of .idea can prevent automatic import of gradle project
   */
  private def deleteIdeaSettings(intelliJ: RunningIntelliJFixture) = {
    val path = intelliJ.workspace.resolve(".idea")
    Option.when(Files.exists(path))(path.delete())
  }

  @ParameterizedTest
  @ValueSource(
    strings = Array("projects/shapeless/ideprobe.conf", "projects/cats/ideprobe.conf", "projects/dokka/ideprobe.conf")
  )
  def verifyModulesPresent(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel()
    val projectModules = project.modules.map(_.name)
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.verify")

    val missingModules = modulesFromConfig.diff(projectModules)
    Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
  }

  @ParameterizedTest
  @ValueSource(
    strings = Array("projects/shapeless/ideprobe.conf", "projects/cats/ideprobe.conf", "projects/dokka/ideprobe.conf")
  )
  def runTestsInModules(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.openProject(intelliJ.workspace)
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.test")
    val moduleRefs = modulesFromConfig.map(ModuleRef(_))
    val runConfigs = moduleRefs.map(JUnitRunConfiguration.module)
    val result = runConfigs.map(config => config.module -> intelliJ.probe.run(config)).toMap

    Assert.assertTrue(s"Tests in modules ${result.values} failed", result.values.forall(_.isSuccess))
  }
}
