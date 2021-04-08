package org.virtuslab.ideprobe

import java.nio.file.Path

import org.virtuslab.ideprobe.config.{IdeProbeConfig, IntelliJProvider}
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}
import org.virtuslab.ideprobe.ide.intellij.{InstalledIntelliJ, RunningIde}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final case class IntelliJFixture(
    workspaceProvider: WorkspaceProvider = WorkspaceTemplate.Empty,
    intelliJProvider: IntelliJProvider = IntelliJProvider.Default,
    plugins: Seq[Plugin] = Nil,
    config: Config = Config.Empty,
    afterWorkspaceSetup: Seq[(IntelliJFixture, Path) => Unit] = Nil,
    afterIntelliJInstall: Seq[(IntelliJFixture, InstalledIntelliJ) => Unit] = Nil,
    afterIntelliJStartup: Seq[(IntelliJFixture, RunningIntelliJFixture) => Unit] = Nil
)(implicit ec: ExecutionContext) {

  def withVmOptions(vmOptions: String*): IntelliJFixture = {
    val newVmOptions = vmOptions ++ intelliJProvider.factory.config.vmOptions
    copy(intelliJProvider = intelliJProvider.withFactory(intelliJProvider.factory.withConfig(config = intelliJProvider.factory.config.copy(vmOptions = newVmOptions))))
  }

  def withEnv(env: Map[String, String]): IntelliJFixture = {
    val newEnv = intelliJProvider.factory.config.env ++ env
    copy(intelliJProvider = intelliJProvider.withFactory(intelliJProvider.factory.withConfig(config = intelliJProvider.factory.config.copy(env = newEnv))))
  }

  def withConfig(entries: (String, String)*): IntelliJFixture = {
    val newConfig = Config.fromMap(entries.toMap).withFallback(config)
    copy(config = newConfig)
  }

  def withPaths(probePaths: IdeProbePaths): IntelliJFixture = {
    copy(intelliJProvider = intelliJProvider.withFactory(intelliJProvider.factory.withPaths(probePaths)))
  }

  def withAfterWorkspaceSetup(action: (IntelliJFixture, Path) => Unit): IntelliJFixture = {
    copy(afterWorkspaceSetup = afterWorkspaceSetup :+ action)
  }

  def withAfterIntelliJInstall(action: (IntelliJFixture, InstalledIntelliJ) => Unit): IntelliJFixture = {
    copy(afterIntelliJInstall = afterIntelliJInstall :+ action)
  }

  def withAfterIntelliJStartup(action: (IntelliJFixture, RunningIntelliJFixture) => Unit): IntelliJFixture = {
    copy(afterIntelliJStartup = afterIntelliJStartup :+ action)
  }

  def withPlugin(plugin: Plugin): IntelliJFixture = {
    copy(plugins = plugin +: plugins)
  }

  def headless: IntelliJFixture = {
    copy(intelliJProvider = intelliJProvider.withFactory(intelliJProvider.factory.withConfig(intelliJProvider.factory.config.copy(headless = true))))
  }

  def run = new SingleRunIntelliJ(this)

  def withWorkspace = new MultipleRunsIntelliJ(this)

  def version: IntelliJVersion = intelliJProvider.version

  def setupWorkspace(): Path = {
    val workspace = workspaceProvider.setup(probePaths).toRealPath()
    afterWorkspaceSetup.foreach(_.apply(this, workspace))
    workspace
  }

  def probePaths: IdeProbePaths = intelliJProvider.factory.paths

  def deleteWorkspace(workspace: Path): Unit = {
    workspaceProvider.cleanup(workspace)
  }

  def installIntelliJ(): InstalledIntelliJ = {
    val installedIntelliJ = intelliJProvider.setup()
    afterIntelliJInstall.foreach(_.apply(this, installedIntelliJ))
    installedIntelliJ
  }

  def cleanupIntelliJ(installedIntelliJ: InstalledIntelliJ): Unit = {
    withRetries(maxRetries = 10)(installedIntelliJ.cleanup())
  }

  def startIntelliJ(workspace: Path, installedIntelliJ: InstalledIntelliJ): RunningIde = {
    val runningIde = installedIntelliJ.startIn(workspace, config)
    val probe = runningIde.probe
    probe.await(WaitLogic.OnStartup)
    val running = new RunningIntelliJFixture(workspace, probe, config, installedIntelliJ.paths)
    afterIntelliJStartup.foreach(_.apply(this, running))
    Runtime.getRuntime.addShutdownHook(new Thread(() => runningIde.shutdown()))
    runningIde
  }

  def closeIntellij(runningIde: RunningIde): Unit = {
    runningIde.shutdown()
  }

  @tailrec
  private def withRetries(maxRetries: Int, delay: FiniteDuration = 1.second)(block: => Unit): Unit = {
    try block
    catch {
      case e: Exception =>
        if (maxRetries == 0) throw e
        else {
          Thread.sleep(delay.toMillis)
          withRetries(maxRetries - 1)(block)
        }
    }
  }

}

object IntelliJFixture {
  private val ConfigRoot = "probe"

  def fromConfig(config: Config, path: String = ConfigRoot)(implicit ec: ExecutionContext): IntelliJFixture = {
    val probeConfig = config[IdeProbeConfig](path)

    new IntelliJFixture(
      workspaceProvider = probeConfig.workspace.map(WorkspaceProvider.from).getOrElse(WorkspaceTemplate.Empty),
      intelliJProvider = IntelliJProvider.from(probeConfig),
      plugins = probeConfig.intellij.plugins.filterNot(_.isInstanceOf[Plugin.Empty]),
      config = config
    )
  }
}
