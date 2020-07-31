package org.virtuslab.ideprobe

import java.util.concurrent.Executors

import org.junit.{Assert, Test}
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}
import org.virtuslab.ideprobe.protocol.{JUnitRunConfiguration, ModuleRef}

import scala.concurrent.ExecutionContext

class Example {
  protected implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  private val dokka = "https://github.com/Kotlin/dokka"
  private val cats = "https://github.com/typelevel/cats"
  private val shapeless = "https://github.com/milessabin/shapeless"

  private val fixture = IntelliJFixture(
    version = IntelliJVersion("202.5792.28-EAP-SNAPSHOT"),
    plugins = List(Plugin("org.intellij.scala", "2020.2.584", Some("nightly")))
  )

  private val config = Config.fromClasspath("SbtProject/ideprobe.conf")
  private val fixtureFromConfig = IntelliJFixture.fromConfig(config)

  def testModuleTestSuiteRun(moduleName: String, intelliJ: RunningIntelliJFixture): Unit = {
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)
    val moduleRef = ModuleRef(moduleName, project.name)
    val runConfig = JUnitRunConfiguration(moduleRef, None, None, None, None)
    val result = intelliJ.probe.run(runConfig)

    Assert.assertTrue("Test failed", result.isSuccess)
  }

  def testModuleImport(modules: String*)(intelliJ: RunningIntelliJFixture): Unit = {
    intelliJ.probe.preconfigureJDK()
    val projectRef = intelliJ.probe.openProject(intelliJ.workspace)
    val project = intelliJ.probe.projectModel(projectRef)

    Assert.assertTrue("Modules are not present", project.modules.map(_.name) containsSlice modules)
  }

  @Test
  def testShapelessModuleTestSuiteRun: Unit =
    fixtureFromConfig
      .copy(workspaceTemplate = WorkspaceTemplate.FromGit(shapeless, None))
      .run(testModuleTestSuiteRun("core-sources", _))

  @Test
  def testCatsModuleTestSuiteRun: Unit =
    fixtureFromConfig
      .copy(workspaceTemplate = WorkspaceTemplate.FromGit(cats, None))
      .run(testModuleTestSuiteRun("tests-sources", _))

  @Test
  def testImportProjectWithoutPlugins: Unit =
    fixture.copy(workspaceTemplate = WorkspaceTemplate.FromGit(dokka, None)).run(testModuleImport("ws"))

  @Test
  def testImportProjectWithPlugin: Unit =
    fixture.copy(workspaceTemplate = WorkspaceTemplate.FromGit(cats, None)).run(testModuleImport("cats-build"))
}
