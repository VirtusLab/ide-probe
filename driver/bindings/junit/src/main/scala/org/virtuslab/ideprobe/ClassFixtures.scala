package org.virtuslab.ideprobe

import java.nio.file.Path

import org.junit.AfterClass
import org.junit.BeforeClass
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.RunningIde

trait RunningIntellijPerSuite {

  private var workspace: Path = _
  private var installed: InstalledIntelliJ = _
  private var running: RunningIde = _
  private var runningIntelliJFixture: RunningIntelliJFixture = _

  final def intelliJ: RunningIntelliJFixture = runningIntelliJFixture

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  @BeforeClass
  final def setup(): Unit = {
    workspace = baseFixture.setupWorkspace()
    installed = baseFixture.installIntelliJ()
    running = baseFixture.startIntelliJ(workspace, installed)
    runningIntelliJFixture =
      new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, baseFixture.test, installed.paths)
    beforeAll()
  }

  @AfterClass
  final def teardown(): Unit = {
    try AfterTestChecks(baseFixture.factory.config.check, runningIntelliJFixture.probe)
    finally {
      try afterAll()
      finally {
        baseFixture.closeIntellij(running)
        baseFixture.deleteIntelliJ(installed)
        baseFixture.deleteWorkspace(workspace)
      }
    }
  }
}

trait WorkspacePerSuite {

  private var workspacePath: Path = _
  private var installed: InstalledIntelliJ = _
  private var runnableIntelliJFixture: RunnableIntelliJFixture = _

  final def workspace: RunnableIntelliJFixture = runnableIntelliJFixture

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  @BeforeClass
  final def setup(): Unit = {
    workspacePath = baseFixture.setupWorkspace()
    installed = baseFixture.installIntelliJ()
    runnableIntelliJFixture = new RunnableIntelliJFixture(workspacePath, installed, baseFixture)
    beforeAll()
  }

  @AfterClass
  final def teardown(): Unit = {
    try afterAll()
    finally {
      baseFixture.deleteIntelliJ(installed)
      baseFixture.deleteWorkspace(workspacePath)
    }
  }
}
