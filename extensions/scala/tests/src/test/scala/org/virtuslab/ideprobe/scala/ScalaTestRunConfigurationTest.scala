package org.virtuslab.ideprobe.scala

import org.junit.{Ignore, Test}
import org.virtuslab.ideprobe.protocol.{ModuleRef, Setting}
import org.virtuslab.ideprobe.scala.protocol.{SbtProjectSettingsChangeRequest, ScalaTestRunConfiguration}

class ScalaTestRunConfigurationTest extends ScalaPluginTestSuite {
  // ignored because it needs a fix in scala plugin included in the nightly 2020.2.801
  // that version doesn't work yet though because sbt-idea-compiler-indices can't be resolved
  @Ignore
  @Test
  def runTestsInDifferentScopes: Unit = fixtureFromConfig("TestConfigurationProject/ideprobe.conf").run { intelliJ =>
    intelliJ.probe.openProject(intelliJ.workspace.resolve("root"))
    intelliJ.probe.setSbtProjectSettings(
      SbtProjectSettingsChangeRequest(
        useSbtShellForImport = Setting.Changed(true),
        useSbtShellForBuild = Setting.Changed(true)
      )
    )

    val moduleName = intelliJ.config[String]("test.module")
    val packageName = intelliJ.config[String]("test.package")
    val className = intelliJ.config[String]("test.class")
    val methodName = intelliJ.config[String]("test.method")
    val moduleRef = ModuleRef(moduleName)

    val moduleRunConfiguration = ScalaTestRunConfiguration.Module(moduleRef)
    val moduleRunResult = intelliJ.probe.run(moduleRunConfiguration)
    assert(moduleRunResult.suites.size == 2)

    val packageRunConfiguration = ScalaTestRunConfiguration.Package(moduleRef, packageName)
    val packageRunResult = intelliJ.probe.run(packageRunConfiguration)
    assert(packageRunResult.suites.size == 2)

    val classRunConfiguration = ScalaTestRunConfiguration.Class(moduleRef, className)
    val classRunResult = intelliJ.probe.run(classRunConfiguration)
    assert(classRunResult.suites.size == 1)

    val methodRunConfiguration = ScalaTestRunConfiguration.Method(moduleRef, className, methodName)
    val methodRunResult = intelliJ.probe.run(methodRunConfiguration)
    assert(methodRunResult.suites.size == 1)
    assert(methodRunResult.suites.head.tests.size == 1)
  }
}
