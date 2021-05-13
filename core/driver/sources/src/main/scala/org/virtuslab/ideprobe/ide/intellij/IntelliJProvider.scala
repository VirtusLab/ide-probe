package org.virtuslab.ideprobe.ide.intellij

import org.virtuslab.ideprobe.IdeProbePaths
import org.virtuslab.ideprobe.config.{IdeProbeConfig, IntellijConfig}
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, InternalPlugins, Plugin}

import java.nio.file.{Files, Path}

trait IntelliJProvider {
  def factory: IntelliJFactory
  def plugins: Seq[Plugin]
  def setup(): InstalledIntelliJ
  def version: IntelliJVersion
  def withFactory(factory: IntelliJFactory): IntelliJProvider
  def withPlugin(plugin: Plugin): IntelliJProvider
}

case class ExistingIntelliJ(
    path: Path,
    plugins: Seq[Plugin],
    factory: IntelliJFactory
) extends IntelliJProvider {
  import org.virtuslab.ideprobe.Extensions._
  override def setup(): InstalledIntelliJ = {
    val pluginsDir = path.resolve("plugins")
    val backupDir = Files.createTempDirectory(path, "plugins")
    pluginsDir.copyDir(backupDir)
    factory.installPlugins(InternalPlugins.probePluginForIntelliJ(version) +: plugins, path)

    new LocalIntelliJ(path, factory.paths, factory.config, backupDir)
  }

  override lazy val version: IntelliJVersion = IntelliJFactory.version(path)

  override def withFactory(factory: IntelliJFactory): IntelliJProvider = copy(factory = factory)

  override def withPlugin(plugin: Plugin): IntelliJProvider = copy(plugins = plugins :+ plugin)
}

case class DefaultIntelliJ(
    version: IntelliJVersion,
    plugins: Seq[Plugin],
    factory: IntelliJFactory
) extends IntelliJProvider {

  override def setup(): InstalledIntelliJ =
    factory.create(version, plugins)

  override def withFactory(factory: IntelliJFactory): IntelliJProvider = copy(factory = factory)

  override def withPlugin(plugin: Plugin): IntelliJProvider = copy(plugins = plugins :+ plugin)
}

object IntelliJProvider {
  val Default = DefaultIntelliJ(
    version = IntelliJVersion.Latest,
    plugins = Seq.empty,
    factory = IntelliJFactory.Default
  )

  def from(config: IdeProbeConfig): IntelliJProvider = {
    val ideProbePaths = IdeProbePaths.from(config.paths)
    val factory = IntelliJFactory.from(config.resolvers, ideProbePaths, config.driver)
    config.intellij match {
      case IntellijConfig.Existing(path, plugins) =>
        ExistingIntelliJ(path, plugins.filterNot(_.isInstanceOf[Plugin.Empty]), factory)
      case IntellijConfig.Default(version, plugins) =>
        DefaultIntelliJ(version, plugins.filterNot(_.isInstanceOf[Plugin.Empty]), factory)
    }
  }
}
