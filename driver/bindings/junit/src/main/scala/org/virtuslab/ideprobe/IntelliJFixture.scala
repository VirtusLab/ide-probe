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
    config: Config
)(implicit ec: ExecutionContext) {

  def withDisplay: IntelliJFixture = {
    copy(factory = factory.withConfig(factory.config.copy(headless = false)))
  }

  def run = new SingleRunIntelliJ(this)

  def withWorkspace = new MultipleRunsIntelliJ(this)

  def setupWorkspace(): Path = {
    val workspaceBase = Files.createTempDirectory("ideprobe-workspace")
    val workspace = workspaceBase.createDirectory("ws")
    workspaceTemplate.setupIn(workspace)
    workspace
  }

  def deleteWorkspace(workspace: Path): Unit = {
    workspace.delete()
  }

  def installIntelliJ(): InstalledIntelliJ = {
    factory.create(version, plugins)
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
    new IntelliJFixture(workspaceTemplate, intelliJFactory, version, plugins, environment)
  }

  def fromConfig(config: Config, path: String = ConfigRoot)(implicit ec: ExecutionContext): IntelliJFixture = {
    import pureconfig.generic.auto._
    val probeConfig = config[IdeProbeConfig](path)
    val factory = IntelliJFactory.from(probeConfig.resolvers, probeConfig.driver)

    new IntelliJFixture(
      workspaceTemplate = probeConfig.workspace.map(WorkspaceTemplate.from).getOrElse(WorkspaceTemplate.Empty),
      factory = factory,
      version = probeConfig.intellij.version,
      plugins = probeConfig.intellij.plugins,
      config = config
    )
  }
}
