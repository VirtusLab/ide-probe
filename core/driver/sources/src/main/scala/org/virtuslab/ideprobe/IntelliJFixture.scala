package org.virtuslab.ideprobe

import java.nio.file.Path

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import org.virtuslab.ideprobe.config.IdeProbeConfig
import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.Plugin
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.IntelliJProvider
import org.virtuslab.ideprobe.ide.intellij.RunningIde

final case class IntelliJFixture(
    workspaceProvider: WorkspaceProvider = WorkspaceTemplate.Empty,
    intelliJProvider: IntelliJProvider = IntelliJProvider.Default,
    config: Config = Config.Empty,
    afterWorkspaceSetup: Seq[(IntelliJFixture, Path) => Unit] = Nil,
    afterIntelliJInstall: Seq[(IntelliJFixture, InstalledIntelliJ) => Unit] = Nil,
    afterIntelliJStartup: Seq[(IntelliJFixture, RunningIntelliJFixture) => Unit] = Nil
)(implicit ec: ExecutionContext) {

  def withVmOptions(vmOptions: String*): IntelliJFixture = {
    val newVmOptions = vmOptions ++ intelliJProvider.config.vmOptions
    copy(
      intelliJProvider = intelliJProvider.withConfig(intelliJProvider.config.copy(vmOptions = newVmOptions))
    )
  }

  def withEnv(env: Map[String, String]): IntelliJFixture = {
    val newEnv = intelliJProvider.config.env ++ env
    copy(
      intelliJProvider = intelliJProvider.withConfig(config = intelliJProvider.config.copy(env = newEnv))
    )
  }

  def withConfig(entries: (String, String)*): IntelliJFixture = {
    val newConfig = Config.fromMap(entries.toMap).withFallback(config)
    copy(config = newConfig)
  }

  def withPaths(probePaths: IdeProbePaths): IntelliJFixture = {
    copy(intelliJProvider = intelliJProvider.withPaths(probePaths))
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
    copy(intelliJProvider = intelliJProvider.withPlugins(plugin))
  }

  def withVersion(intelliJVersion: IntelliJVersion): IntelliJFixture =
    copy(intelliJProvider = intelliJProvider.withVersion(intelliJVersion))

  def headless: IntelliJFixture = {
    copy(
      intelliJProvider = intelliJProvider.withConfig(intelliJProvider.config.copy(headless = true))
    )
  }

  def run = new SingleRunIntelliJ(this)

  def withWorkspace = new MultipleRunsIntelliJ(this)

  def version: IntelliJVersion = intelliJProvider.version

  def setupWorkspace(): Path = {
    val workspace = workspaceProvider.setup(probePaths).toRealPath()
    afterWorkspaceSetup.foreach(_.apply(this, workspace))
    workspace
  }

  def probePaths: IdeProbePaths = intelliJProvider.paths

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
  lazy val defaultConfig: IdeProbeConfig = readIdeProbeConfig(Config.fromReferenceConf, ConfigRoot)

  def fromConfig(config: Config, path: String = ConfigRoot)(implicit ec: ExecutionContext): IntelliJFixture = {
    val probeConfig = readIdeProbeConfig(config, path)

    new IntelliJFixture(
      workspaceProvider = probeConfig.workspace.map(WorkspaceProvider.from).getOrElse(WorkspaceTemplate.Empty),
      intelliJProvider = getIntelliJProvider(probeConfig),
      config = config
    )
  }

  def readIdeProbeConfig(config: Config, path: String): IdeProbeConfig = config[IdeProbeConfig](path)

  def getIntelliJProvider(probeConfig: IdeProbeConfig): IntelliJProvider = IntelliJProvider
    .from(probeConfig.intellij, probeConfig.resolvers, IdeProbePaths.from(probeConfig.paths), probeConfig.driver)
}
