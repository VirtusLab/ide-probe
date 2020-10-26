package org.virtuslab.ideprobe

import java.nio.file.Path

import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.IntelliJPaths
import org.virtuslab.ideprobe.reporting.AfterTestChecks

final class RunningIntelliJFixture(
    val workspace: Path,
    val probe: ProbeDriver,
    val config: Config,
    val intelliJPaths: IntelliJPaths
)

final class RunnableIntelliJFixture(
    val path: Path,
    installedIntelliJ: InstalledIntelliJ,
    fixture: IntelliJFixture
) {
  def runShell(args: Seq[String]): CommandResult = Shell.run(path, args: _*)

  def config: Config = fixture.config

  def intelliJPaths: IntelliJPaths = installedIntelliJ.intellijPaths

  def runIntellij[A](action: RunningIntelliJFixture => A): A = {
    val running = fixture.startIntelliJ(path, installedIntelliJ)
    val data = new RunningIntelliJFixture(path, running.probe, config, intelliJPaths)

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
    val data = new RunningIntelliJFixture(workspace, running.probe, baseFixture.config, installed.intellijPaths)

    try {
      try action(data)
      finally reporting.AfterTestChecks(baseFixture.factory.config.check, data.probe)
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
