package org.virtuslab.ideprobe

import java.nio.file.Path

import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.RunningIde
import org.virtuslab.ideprobe.reporting.AfterTestChecks

trait RunningIntelliJPerSuiteBase {

  private var workspace: Option[Path] = None
  private var installed: Option[InstalledIntelliJ] = None
  private var running: Option[RunningIde] = None
  private var runningIntelliJFixture: Option[RunningIntelliJFixture] = None

  final def intelliJ: RunningIntelliJFixture = runningIntelliJFixture.getOrElse(
    error("Intellij Fixture not initialized")
  )

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  def setup(): Unit = {
    val workspace = baseFixture.setupWorkspace()
    this.workspace = Some(workspace)
    val installed = baseFixture.installIntelliJ()
    this.installed = Some(installed)
    val running = baseFixture.startIntelliJ(workspace, installed)
    this.running = Some(running)
    val runningIntelliJFixture = new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, installed.paths)
    this.runningIntelliJFixture = Some(runningIntelliJFixture)
    beforeAll()
  }

  def teardown(): Unit = {
    try runningIntelliJFixture.foreach( r => AfterTestChecks(baseFixture.factory.config.check, r.probe))
    finally {
      try afterAll()
      finally {
        running.foreach(baseFixture.closeIntellij(_))
        installed.foreach(baseFixture.deleteIntelliJ(_))
        workspace.foreach(baseFixture.deleteWorkspace(_))
      }
    }
  }
}

trait WorkspacePerSuiteBase {

  private var workspacePath: Option[Path] = None
  private var installed: Option[InstalledIntelliJ] = None
  private var runnableIntelliJFixture: Option[RunnableIntelliJFixture] = None

  final def workspace: RunnableIntelliJFixture =  runnableIntelliJFixture.getOrElse(
    error("Intellij Fixture not initialized")
  )

  protected def baseFixture: IntelliJFixture

  protected def beforeAll(): Unit = ()

  protected def afterAll(): Unit = ()

  def setup(): Unit = {
    val workspacePath = baseFixture.setupWorkspace()
    this.workspacePath = Some(workspacePath)
    val installed = baseFixture.installIntelliJ()
    this.installed = Some(installed)
    val runnableIntelliJFixture = new RunnableIntelliJFixture(workspacePath, installed, baseFixture)
    this.runnableIntelliJFixture = Some(runnableIntelliJFixture)
    beforeAll()
  }

  def teardown(): Unit = {
    try afterAll()
    finally {
      installed.foreach(baseFixture.deleteIntelliJ(_))
      workspacePath.foreach(baseFixture.deleteWorkspace(_))
    }
  }
}
