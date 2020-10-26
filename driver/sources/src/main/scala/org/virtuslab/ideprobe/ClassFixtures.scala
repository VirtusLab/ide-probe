package org.virtuslab.ideprobe

import java.nio.file.Path

import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.RunningIde
import org.virtuslab.ideprobe.reporting.AfterTestChecks

trait RunningIntelliJPerSuiteBase {

  private var workspace: Path = _
  private var installed: InstalledIntelliJ = _
  private var running: RunningIde = _
  private var runningIntelliJFixture: RunningIntelliJFixture = _

  final def intelliJ: RunningIntelliJFixture = runningIntelliJFixture

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  def setup(): Unit = {
    workspace = baseFixture.setupWorkspace()
    installed = baseFixture.installIntelliJ()
    running = baseFixture.startIntelliJ(workspace, installed)
    runningIntelliJFixture = new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, installed.intellijPaths)
    beforeAll()
  }

  def teardown(): Unit = {
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

trait WorkspacePerSuiteBase {

  private var workspacePath: Path = _
  private var installed: InstalledIntelliJ = _
  private var runnableIntelliJFixture: RunnableIntelliJFixture = _

  final def workspace: RunnableIntelliJFixture = runnableIntelliJFixture

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  def setup(): Unit = {
    workspacePath = baseFixture.setupWorkspace()
    installed = baseFixture.installIntelliJ()
    runnableIntelliJFixture = new RunnableIntelliJFixture(workspacePath, installed, baseFixture)
    beforeAll()
  }

  def teardown(): Unit = {
    try afterAll()
    finally {
      baseFixture.deleteIntelliJ(installed)
      baseFixture.deleteWorkspace(workspacePath)
    }
  }
}
