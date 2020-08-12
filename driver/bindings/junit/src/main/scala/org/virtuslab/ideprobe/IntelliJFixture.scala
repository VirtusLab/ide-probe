package org.virtuslab.ideprobe

import java.nio.file.Files
import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.Plugin
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import org.virtuslab.ideprobe.ide.intellij.IntelliJFactory
import org.virtuslab.ideprobe.ide.intellij.RunningIde
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final case class IntelliJFixture(
    workspaceTemplate: WorkspaceTemplate,
    factory: IntelliJFactory,
    version: IntelliJVersion,
    plugins: Seq[Plugin],
    config: Config,
    afterWorkspaceSetup: Seq[(IntelliJFixture, Path) => Unit],
    afterIntelliJInstall: Seq[(IntelliJFixture, InstalledIntelliJ) => Unit]
)(implicit ec: ExecutionContext) {

  def withConfig(entries: (String, String)*): IntelliJFixture = {
    val newConfig = Config.fromMap(entries.toMap).withFallback(config)
    copy(config = newConfig)
  }

  def withAfterWorkspaceSetup(action: (IntelliJFixture, Path) => Unit): IntelliJFixture = {
    copy(afterWorkspaceSetup = afterWorkspaceSetup :+ action)
  }

  def withAfterIntelliJInstall(action: (IntelliJFixture, InstalledIntelliJ) => Unit): IntelliJFixture = {
    copy(afterIntelliJInstall = afterIntelliJInstall :+ action)
  }

  def withPlugin(plugin: Plugin): IntelliJFixture = {
    copy(plugins = plugin +: plugins)
  }

  def headless: IntelliJFixture = {
    copy(factory = factory.withConfig(factory.config.copy(headless = true)))
  }

  def run = new SingleRunIntelliJ(this)

  def withWorkspace = new MultipleRunsIntelliJ(this)

  def setupWorkspace(): Path = {
    val workspaceBase = Files.createTempDirectory("ideprobe-workspace")
    val workspace = workspaceBase.createDirectory("ws")
    workspaceTemplate.setupIn(workspace)
    afterWorkspaceSetup.foreach(_.apply(this, workspace))
    workspace
  }

  def deleteWorkspace(workspace: Path): Unit = {
    workspace.delete()
  }

  def installIntelliJ(): InstalledIntelliJ = {
    val installedIntelliJ = factory.create(version, plugins)
    afterIntelliJInstall.foreach(_.apply(this, installedIntelliJ))
    installedIntelliJ
  }

  def deleteIntelliJ(installedIntelliJ: InstalledIntelliJ): Unit = {
    withRetries(maxRetries = 10)(installedIntelliJ.root.delete())
  }

  def startIntelliJ(workspace: Path, installedIntelliJ: InstalledIntelliJ): RunningIde = {
    val runningIde = installedIntelliJ.startIn(workspace)
    val probe = runningIde.probe
    probe.awaitIdle()
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

  def apply(
      workspaceTemplate: WorkspaceTemplate = WorkspaceTemplate.Empty,
      version: IntelliJVersion = IntelliJVersion.Latest,
      intelliJFactory: IntelliJFactory = IntelliJFactory.Default,
      plugins: Seq[Plugin] = Seq.empty,
      environment: Config = Config.Empty
  )(implicit ec: ExecutionContext): IntelliJFixture = {
    new IntelliJFixture(
      workspaceTemplate,
      intelliJFactory,
      version,
      plugins,
      environment,
      afterWorkspaceSetup = Nil,
      afterIntelliJInstall = Nil
    )
  }

  def fromConfig(config: Config, path: String = ConfigRoot)(implicit ec: ExecutionContext): IntelliJFixture = {
    val probeConfig = config[IdeProbeConfig](path)

    new IntelliJFixture(
      workspaceTemplate = probeConfig.workspace.map(WorkspaceTemplate.from).getOrElse(WorkspaceTemplate.Empty),
      factory = IntelliJFactory.from(probeConfig.resolvers, probeConfig.driver),
      version = probeConfig.intellij.version,
      plugins = probeConfig.intellij.plugins.filterNot(_.isInstanceOf[Plugin.Empty]),
      config = config,
      afterWorkspaceSetup = Nil,
      afterIntelliJInstall = Nil
    )
  }
}
