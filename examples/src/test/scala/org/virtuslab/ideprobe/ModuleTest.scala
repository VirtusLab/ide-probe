package org.virtuslab.ideprobe

import java.nio.file.Files

import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.virtuslab.ideprobe.protocol.{BuildScope, JUnitRunConfiguration, ModuleRef, Setting}
import org.virtuslab.ideprobe.scala.ScalaTestSuite
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

class ModuleTest extends ScalaTestSuite {
  @ParameterizedTest
  @ValueSource(
    strings = Array(
      "projects/shapeless.conf",
      "projects/cats.conf",
      "projects/dokka.conf"
    )
  )
  def verifyModulesPresent(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel()
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.verify")
    val missingModules = modulesFromConfig.diff(project.moduleNames)

    Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
  }

  @ParameterizedTest
  @ValueSource(
    strings = Array(
      "projects/shapeless.conf",
      "projects/cats.conf",
      "projects/dokka.conf"
    )
  )
  def runTestsInModules(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    deleteIdeaSettings(intelliJ)
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    if (Files.exists(intelliJ.workspace.resolve("build.sbt"))) {
      intelliJ.probe.setSbtProjectSettings(
        SbtProjectSettingsChangeRequest(
          useSbtShellForImport = Setting.Changed(true),
          useSbtShellForBuild = Setting.Changed(true)
        )
      )
    }
    val modulesFromConfig = intelliJ.config[Seq[String]]("modules.test")
    val moduleRefs = modulesFromConfig.map(ModuleRef(_))
    val runConfigs = moduleRefs.map(JUnitRunConfiguration.module)
    val buildResult = intelliJ.probe.build(BuildScope.modules(projectRef, modulesFromConfig: _*))
    buildResult.assertSuccess()
    runConfigs.map(config => config.module -> intelliJ.probe.run(config)).foreach {
      case (module, result) => Assert.assertTrue(s"Tests in module ${module} failed", result.isSuccess)
    }
  }
}
