package org.virtuslab.ideprobe

import java.nio.file.Files
import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.{ModuleRef, Setting, TestRunConfiguration}
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.scala.ScalaPluginExtension
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

class ModuleTest extends IdeProbeFixture with ScalaPluginExtension with RobotPluginExtension {
  @ParameterizedTest
  @ValueSource(
    strings = Array(
      "projects/io.conf",
      "projects/librarymanagement.conf",
      "projects/dokka.conf"
    )
  )
  def verifyModulesPresent(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.withRobot.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel()
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.verify")
    val missingModules = modulesFromConfig.diff(project.moduleNames)
    Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
  }

  @ParameterizedTest
  @ValueSource(
    strings = Array(
      "projects/io.conf",
      "projects/librarymanagement.conf",
      "projects/dokka.conf"
    )
  )
  def runTestsInModules(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.withRobot.openProject(intelliJ.workspace)
    if (Files.exists(intelliJ.workspace.resolve("build.sbt"))) {
      intelliJ.probe.setSbtProjectSettings(
        SbtProjectSettingsChangeRequest(
          useSbtShellForImport = Setting.Changed(true),
          useSbtShellForBuild = Setting.Changed(true)
        )
      )
    }
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.test")
    val runnerNameFragmentOpt = intelliJ.config.get[String]("runner")
    val moduleRefs = modulesFromConfig.map(ModuleRef(_))
    val runConfigs = moduleRefs.map(moduleRef => TestRunConfiguration(moduleRef, runnerNameFragmentOpt))
    runConfigs.map(config => config.module -> intelliJ.probe.run(config)).foreach {
      case (module, result) => Assert.assertTrue(s"Tests in module $module failed", result.isSuccess)
    }
  }

  /**
   * The presence of .idea can prevent automatic import of gradle project
   */
  protected def deleteIdeaSettings(intelliJ: RunningIntelliJFixture): Unit = {
    val path = intelliJ.workspace.resolve(".idea")
    if (Files.exists(path)) path.delete()
  }

}
