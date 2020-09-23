package org.virtuslab.ideprobe

import java.nio.file.Files

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.protocol._
import org.virtuslab.ideprobe.scala.ScalaPluginExtension
import org.virtuslab.ideprobe.scala.protocol.{SbtProjectSettingsChangeRequest, ScalaTestRunConfiguration}

class ModuleTest extends IdeProbeFixture with ScalaPluginExtension with RobotPluginExtension {
  @Test def runTestsInDifferentScopes: Unit = fixtureFromConfig("projects/dokka.conf").run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.openProject(intelliJ.workspace)
    val buildResult = intelliJ.probe.build()
    buildResult.assertSuccess()

    val moduleName = intelliJ.config[String]("test.module")
    val packageName = intelliJ.config[String]("test.package")
    val directoryName = intelliJ.config[String]("test.directory")
    val className = intelliJ.config[String]("test.class")
    val methodName = intelliJ.config[String]("test.method")
    val moduleRef = ModuleRef(moduleName)

    val runConfigurations = List(
      TestRunConfiguration.Module(moduleRef),
      TestRunConfiguration.Directory(moduleRef, directoryName),
      TestRunConfiguration.Package(moduleRef, packageName),
      TestRunConfiguration.Class(moduleRef, className),
      TestRunConfiguration.Method(moduleRef, className, methodName)
    )

    runConfigurations.map(intelliJ.probe.run(_, None)).foreach { result =>
      Assert.assertTrue(s"Test result $result should not be empty", result.suites.nonEmpty)
    }
  }

  @Test
  def runScalaTestTestsInDifferentScopes: Unit = fixtureFromConfig("projects/io.conf").run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.openProject(intelliJ.workspace)
    useSbtShell(intelliJ)

    val moduleName = intelliJ.config[String]("test.module")
    val packageName = intelliJ.config[String]("test.package")
    val className = intelliJ.config[String]("test.class")
    val methodName = intelliJ.config[String]("test.method")
    val moduleRef = ModuleRef(moduleName)

    val runConfigurations = List(
      ScalaTestRunConfiguration.Module(moduleRef),
      ScalaTestRunConfiguration.Package(moduleRef, packageName),
      ScalaTestRunConfiguration.Class(moduleRef, className),
      ScalaTestRunConfiguration.Method(moduleRef, className, methodName)
    )

    runConfigurations.map(intelliJ.probe.run(_)).foreach { result =>
      Assert.assertTrue(s"Test result $result should not be empty", result.suites.nonEmpty)
    }
  }

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
    useSbtShell(intelliJ)
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.test")
    val runnerNameFragmentOpt = intelliJ.config.get[String]("runner")
    val moduleRefs = modulesFromConfig.map(ModuleRef(_))
    val runConfigs = moduleRefs.map(moduleRef => TestRunConfiguration.Module(moduleRef))
    runConfigs.map(config => config.module -> intelliJ.probe.run(config, runnerNameFragmentOpt)).foreach {
      case (module, result) => Assert.assertTrue(s"Tests in module $module failed", result.isSuccess)
    }
  }

  protected def useSbtShell(intelliJ: RunningIntelliJFixture): Unit = {
    if (Files.exists(intelliJ.workspace.resolve("build.sbt"))) {
      intelliJ.probe.setSbtProjectSettings(
        SbtProjectSettingsChangeRequest(
          useSbtShellForImport = Setting.Changed(true),
          useSbtShellForBuild = Setting.Changed(true)
        )
      )
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
