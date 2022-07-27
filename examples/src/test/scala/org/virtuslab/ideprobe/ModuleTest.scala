package org.virtuslab.ideprobe

import org.junit.Assert
import org.junit.Test

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol._
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.scala.ScalaPluginExtension
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

class ModuleTest extends IdeProbeFixture with ScalaPluginExtension with RobotPluginExtension {

  registerFixtureTransformer(_.withAfterIntelliJStartup((fixture, intelliJ) => {
    deleteIdeaSettings(intelliJ)
    intelliJ.probe.withRobot.openProject(intelliJ.workspace)
    useSbtShell(intelliJ)
  }))

  @Test def runTestsInIOScope(): Unit = runTestsInDifferentScopes("projects/io.conf")
  @Test def runTestsInLibraryManagementScope(): Unit = runTestsInDifferentScopes("projects/librarymanagement.conf")
  @Test def runTestsInDokkaScope(): Unit = runTestsInDifferentScopes("projects/dokka.conf")

  private def runTestsInDifferentScopes(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    val runnerToSelect = intelliJ.config.get[String]("runner")
    val modulesToTest = intelliJ.config[Seq[String]]("modules.test")
    intelliJ.probe.build().assertSuccess()
    modulesToTest.foreach { moduleName =>
      val scope = TestScope.Module(ModuleRef(moduleName))
      val result = intelliJ.probe.runTestsFromGenerated(scope, runnerToSelect, None)
      Assert.assertTrue(s"Test result $result should not be empty", result.suites.nonEmpty)
    }
  }

  @Test def verifyModuleIO(): Unit = verifyModulesPresent("projects/io.conf")
  @Test def verifyModuleLibraryManagement(): Unit = verifyModulesPresent("projects/librarymanagement.conf")
  @Test def verifyModuleDokka(): Unit = verifyModulesPresent("projects/dokka.conf")

  private def verifyModulesPresent(configName: String): Unit = fixtureFromConfig(configName).run { intelliJ =>
    val project = intelliJ.probe.projectModel()
    val expectedModules = intelliJ.config[Seq[String]]("modules.verify")
    val missingModules = expectedModules.diff(project.moduleNames)
    Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
  }

  protected def useSbtShell(intelliJ: RunningIntelliJFixture): Unit = {
    if (intelliJ.workspace.resolve("build.sbt").isFile) {
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
  private def deleteIdeaSettings(intelliJ: RunningIntelliJFixture): Unit = {
    val path = intelliJ.workspace.resolve(".idea")
    if (path.isDirectory) path.delete()
  }

}
