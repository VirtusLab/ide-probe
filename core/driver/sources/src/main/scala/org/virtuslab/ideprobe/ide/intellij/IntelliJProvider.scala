package org.virtuslab.ideprobe.ide.intellij

import java.nio.file.{Files, Path, Paths}
import java.util.stream.{Collectors, Stream => JStream}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe._
import org.virtuslab.ideprobe.config.{DependenciesConfig, DriverConfig, IntellijConfig}
import org.virtuslab.ideprobe.dependencies._
import org.virtuslab.ideprobe.dependencies.Resource._

sealed trait IntelliJProvider {
  def version: IntelliJVersion
  def plugins: Seq[Plugin]
  def config: DriverConfig
  def paths: IdeProbePaths
  def withVersion(version: IntelliJVersion): IntelliJProvider
  def withConfig(config: DriverConfig): IntelliJProvider
  def withPaths(paths: IdeProbePaths): IntelliJProvider
  def withPlugins(plugins: Plugin*): IntelliJProvider
  def setup(): InstalledIntelliJ

  // This method differentiates plugins by their root entries in zip
  // assuming that plugins with same root entries are the same plugin
  // and only installs last occurrance of such plugin in the list
  // in case of duplicates.
  protected def installPlugins(
      dependencies: DependencyProvider,
      plugins: Seq[Plugin],
      intelliJ: InstalledIntelliJ
  ): Unit = {
    val allPlugins = InternalPlugins.probePluginForIntelliJ(version) +: plugins

    case class PluginArchive(plugin: Plugin, archive: Resource.IntellijResource) {
      val rootEntries: Set[String] = archive.rootEntries.toSet
    }

    val targetDir = intelliJ.paths.bundledPlugins
    val archives = withParallel[Plugin, PluginArchive](allPlugins)(_.map { plugin =>
      val fileOpt = dependencies.plugin.fetch(plugin)
      fileOpt match {
        case Some(file) => PluginArchive(plugin, file.toExtracted)
        case _ => error("Plugin archive not found")

      }
    })

    val distinctPlugins = archives.reverse.distinctBy(_.rootEntries).reverse

    parallel(distinctPlugins).forEach { pluginArchive =>
      pluginArchive.archive.installTo(targetDir)
      println(s"Installed ${pluginArchive.plugin}")
    }
  }

  private def withParallel[A, B](s: Seq[A])(f: JStream[A] => JStream[B]): Seq[B] = {
    f(parallel(s)).collect(Collectors.toList[B]).asScala.toList
  }

  private def parallel[A](s: Seq[A]) = {
    s.asJava.parallelStream
  }
}

final case class ExistingIntelliJ(
    dependencies: DependencyProvider,
    path: Path,
    plugins: Seq[Plugin],
    paths: IdeProbePaths,
    config: DriverConfig
) extends IntelliJProvider {
  override val version = IntelliJVersionResolver.version(path)

  override def withVersion(version: IntelliJVersion): IntelliJProvider =
    error("Cannot set version for existing IntelliJ instance")

  override def withConfig(config: DriverConfig): ExistingIntelliJ =
    copy(config = config)

  override def withPaths(paths: IdeProbePaths): ExistingIntelliJ =
    copy(paths = paths)

  override def withPlugins(plugins: Plugin*): ExistingIntelliJ =
    copy(plugins = this.plugins ++ plugins)

  override def setup(): InstalledIntelliJ = {
    val intelliJPaths = IntelliJPaths.fromExistingInstance(path)
    val pluginsDir = intelliJPaths.bundledPlugins
    val backupDir = Files.createTempDirectory(path, "plugins")
    val intelliJ = new LocalIntelliJ(path, paths, config, intelliJPaths, backupDir)
    pluginsDir.copyDir(backupDir)
    installPlugins(dependencies, plugins, intelliJ)

    intelliJ
  }
}

final case class IntelliJFactory(
    dependencies: DependencyProvider,
    plugins: Seq[Plugin],
    version: IntelliJVersion,
    paths: IdeProbePaths,
    config: DriverConfig
) extends IntelliJProvider {

  override def withVersion(version: IntelliJVersion): IntelliJProvider =
    copy(version = version)

  override def withConfig(config: DriverConfig): IntelliJFactory =
    copy(config = config)

  override def withPaths(paths: IdeProbePaths): IntelliJFactory =
    copy(paths = paths)

  override def withPlugins(plugins: Plugin*): IntelliJProvider =
    copy(plugins = this.plugins ++ plugins)

  override def setup(): InstalledIntelliJ = {
    val root = createInstanceDirectory(version)

    val intelliJPaths: IntelliJPaths = IntelliJPaths.default(root)
    installIntelliJ(version, root)
    val intelliJ = new DownloadedIntelliJ(root, paths, intelliJPaths, config)
    installJbr(dependencies, intelliJ)
    installPlugins(dependencies, plugins, intelliJ)

    intelliJ
  }

  private def installJbr(dependencies: DependencyProvider, intelliJ: DownloadedIntelliJ): Unit = {
    dependencies.jbr.fetchOpt(intelliJ.paths.root).foreach { jbrArchive =>
      val archive = jbrArchive.toString
      val output = intelliJ.paths.root.createDirectory("jbr")
      SilentShell.run("tar", "-xvzf", archive, "-C", output.toString, "--strip-components=1").assertSuccess()
    }
  }

  private def createInstanceDirectory(version: IntelliJVersion): Path = {
    val base = paths.instances.createTempDirectory(s"intellij-instance-${version.build}-")
    if (OS.Current == OS.Mac) {
      base.createDirectory("Contents")
    } else {
      base
    }
  }

  private def installIntelliJ(version: IntelliJVersion, root: Path): Unit = {
    println(s"Installing $version")
    val fileOpt = dependencies.intelliJ.fetch(version)
    fileOpt match {
      case Some(file) =>
        file.toExtracted.installTo(root)
        root.resolve("bin").makeExecutableRecursively()
      case None => error("Intellij artifacts not found")
    }
  }
}

object IntelliJProvider {
  val Default =
    IntelliJFactory(
      dependencies = new DependencyProvider(
        new IntelliJDependencyProvider(Seq(IntelliJZipResolver.community), ResourceProvider.Default),
        new PluginDependencyProvider(Seq(PluginResolver.Official), ResourceProvider.Default),
        new JbrDependencyProvider(Seq(JbrResolvers.official), ResourceProvider.Default)
      ),
      plugins = Seq.empty,
      version = IntelliJVersion.Latest,
      paths = IdeProbePaths.Default,
      config = DriverConfig()
    )

  def from(
      intelliJConfig: IntellijConfig,
      resolversConfig: DependenciesConfig.Resolvers,
      paths: IdeProbePaths,
      driverConfig: DriverConfig
  ): IntelliJProvider = {
    val intelliJResolvers = IntelliJZipResolver.fromConfig(resolversConfig.intellij)
    val pluginResolver = PluginResolver.fromConfig(resolversConfig.plugins)
    val jbrResolvers = JbrResolvers.fromConfig(resolversConfig.jbr)
    val resourceProvider = ResourceProvider.fromConfig(paths)
    val intelliJDependencyProvider = new IntelliJDependencyProvider(intelliJResolvers, resourceProvider)
    val pluginDependencyProvider = new PluginDependencyProvider(Seq(pluginResolver), resourceProvider)
    val jbrDependencyProvider = new JbrDependencyProvider(jbrResolvers, resourceProvider)
    val dependencyProvider =
      new DependencyProvider(intelliJDependencyProvider, pluginDependencyProvider, jbrDependencyProvider)
    intelliJConfig match {
      case IntellijConfig.Default(version, plugins) =>
        IntelliJFactory(
          dependencyProvider,
          plugins.filterNot(_.isInstanceOf[Plugin.Empty]),
          version,
          paths,
          driverConfig
        )
      case IntellijConfig.Existing(path, plugins) =>
        ExistingIntelliJ(
          dependencyProvider,
          path,
          plugins.filterNot(_.isInstanceOf[Plugin.Empty]),
          paths,
          driverConfig
        )
    }
  }
}
