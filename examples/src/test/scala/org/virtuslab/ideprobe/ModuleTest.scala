package org.virtuslab.ideprobe

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}
import org.virtuslab.ideprobe.protocol._
import org.virtuslab.ideprobe.robot.RobotPluginExtension
import org.virtuslab.ideprobe.scala.ScalaPluginExtension
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

@RunWith(classOf[Parameterized])
class ModuleTest(configName: String) extends IdeProbeFixture with ScalaPluginExtension with RobotPluginExtension {
    registerFixtureTransformer(_.withAfterIntelliJStartup((fixture, intelliJ) => {
      deleteIdeaSettings(intelliJ)
      intelliJ.probe.withRobot.openProject(intelliJ.workspace)
      useSbtShell(intelliJ)
    }))

    @Test def verifyModulesPresent: Unit = fixtureFromConfig(configName).run { intelliJ =>
      val project = intelliJ.probe.projectModel()
      val expectedModules = intelliJ.config[Seq[String]]("modules.verify")
      val missingModules = expectedModules.diff(project.moduleNames)
      Assert.assertTrue(s"Modules $missingModules are missing", missingModules.isEmpty)
    }

    @Test def runTestsInDifferentScopes: Unit = fixtureFromConfig(configName).run { intelliJ =>
      val runnerToSelect = intelliJ.config.get[String]("runner")
      val modulesToTest = intelliJ.config[Seq[String]]("modules.test")
      intelliJ.probe.build().assertSuccess()
      modulesToTest.foreach { moduleName =>
        val scope = TestScope.Module(ModuleRef(moduleName))
        val result = intelliJ.probe.runTestsFromGenerated(scope, runnerToSelect)
        Assert.assertTrue(s"Test result $result should not be empty", result.suites.nonEmpty)
      }
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

object ModuleTest {
  @Parameters(name = "{0}") def data: java.util.Collection[String] = {
    java.util.Arrays.asList("projects/io.conf", "projects/librarymanagement.conf", "projects/dokka.conf")
  }
}
