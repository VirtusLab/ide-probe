package com.virtuslab.ideprobe

import java.nio.file.Path

import com.virtuslab.ideprobe.Shell.CommandResult
import com.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import com.virtuslab.ideprobe.ide.intellij.IntelliJPaths

final class RunningIntelliJFixture(
    val workspace: Path,
    val probe: ProbeDriver,
    val config: Config,
    val intelliJPaths: IntelliJPaths
)

final class RunnableIntelliJFixture(
    val workspace: Path,
    installedIntelliJ: InstalledIntelliJ,
    fixture: IntelliJFixture
) {
  def runShell(args: Seq[String]): CommandResult = Shell.run(workspace, args: _*)

  def config: Config = fixture.config

  def intelliJPaths: IntelliJPaths = installedIntelliJ.paths

  def run[A](action: RunningIntelliJFixture => A): A = {
    val running = fixture.startIntelliJ(workspace, installedIntelliJ)
    val data = new RunningIntelliJFixture(workspace, running.probe, config, intelliJPaths)

    try {
      try action(data)
      finally AfterTestChecks(fixture.factory.config.check, data.probe)
    } finally fixture.closeIntellij(running)
  }
}

class SingleRunIntelliJ(baseFixture: IntelliJFixture) {
  def apply[A](action: RunningIntelliJFixture => A): A = {
    val workspace = baseFixture.setupWorkspace()
    val installed = baseFixture.installIntelliJ()
    val running = baseFixture.startIntelliJ(workspace, installed)
    val data = new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, installed.paths)

    try {
      try action(data)
      finally AfterTestChecks(baseFixture.factory.config.check, data.probe)
    } finally {
      baseFixture.closeIntellij(running)
      baseFixture.deleteIntelliJ(installed)
      baseFixture.deleteWorkspace(workspace)
    }
  }
}

class MultipleRunsIntelliJ(baseFixture: IntelliJFixture) {
  def apply[A](action: RunnableIntelliJFixture => A): A = {
    val workspace = baseFixture.setupWorkspace()
    val installed = baseFixture.installIntelliJ()
    val fixture = new RunnableIntelliJFixture(workspace, installed, baseFixture)
    try action(fixture)
    finally {
      baseFixture.deleteIntelliJ(installed)
      baseFixture.deleteWorkspace(workspace)
    }
  }
}
