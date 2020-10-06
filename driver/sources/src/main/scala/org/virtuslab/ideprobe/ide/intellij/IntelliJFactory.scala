package org.virtuslab.ideprobe.ide.intellij

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.{Stream => JStream}
import org.virtuslab.ideprobe.BuildInfo
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.config.DependenciesConfig
import org.virtuslab.ideprobe.config.DriverConfig
import org.virtuslab.ideprobe.dependencies.{
  DependencyProvider,
  IntelliJDependencyProvider,
  IntelliJVersion,
  IntelliJZipResolver,
  InternalPlugins,
  Plugin,
  PluginDependencyProvider,
  PluginResolver,
  Resource,
  ResourceProvider
}

final class IntelliJFactory(
    dependencies: DependencyProvider,
    val config: DriverConfig
) {
  def withConfig(config: DriverConfig): IntelliJFactory = new IntelliJFactory(dependencies, config)

  def create(version: IntelliJVersion, plugins: Seq[Plugin]): InstalledIntelliJ = {
    val root = Files.createTempDirectory(s"intellij-instance-${version.build}-")
    val allPlugins = InternalPlugins.all ++ plugins

    installIntelliJ(version, root)
    installPlugins(allPlugins, root)

    new InstalledIntelliJ(root, config)
  }

  private def installIntelliJ(version: IntelliJVersion, root: Path): Unit = {
    println(s"Installing $version")
    val file = dependencies.fetch(version)
    toArchive(file).extractTo(root)
    root.resolve("bin/linux/fsnotifier64").makeExecutable()
    println(s"Installed $version")
  }

  // This method differentiates plugins by their root entries in zip
  // assuming that plugins with same root entries are the same plugin
  // and only installs last occurrance of such plugin in the list
  // in case of duplicates.
  private def installPlugins(plugins: Seq[Plugin], root: Path): Unit = {
    case class PluginArchive(plugin: Plugin, archive: Resource.Archive) {
      val rootEntries: Set[String] = archive.rootEntries.toSet
    }

    val targetDir = root.resolve("plugins")
    val archives = withParallel[Plugin, PluginArchive](plugins)(_.map { plugin =>
      val file = dependencies.fetch(plugin)
      PluginArchive(plugin, toArchive(file))
    })

    val distinctPlugins = archives.reverse.distinctBy(_.rootEntries).reverse

    parallel(distinctPlugins).forEach { pluginArchive =>
      pluginArchive.archive.extractTo(targetDir)
      println(s"Installed ${pluginArchive.plugin}")
    }
  }

  private def toArchive(resource: Path): Resource.Archive = {
    resource match {
      case Resource.Archive(archive) => archive
      case _                         => throw new IllegalStateException(s"Not an archive: $resource")
    }
  }

  private def withParallel[A, B](s: Seq[A])(f: JStream[A] => JStream[B]): Seq[B] = {
    f(parallel(s)).collect(Collectors.toList[B]).asScala.toList
  }

  private def parallel[A](s: Seq[A]) = {
    s.asJava.parallelStream
  }
}

object IntelliJFactory {
  val Default =
    new IntelliJFactory(
      new DependencyProvider(
        new IntelliJDependencyProvider(IntelliJZipResolver.Community, ResourceProvider.Default),
        new PluginDependencyProvider(PluginResolver.Official, ResourceProvider.Default)
      ),
      DriverConfig()
    )

  def from(resolversConfig: DependenciesConfig.Resolvers, driverConfig: DriverConfig): IntelliJFactory = {
    val intelliJResolver = IntelliJZipResolver.from(resolversConfig.intellij)
    val pluginResolver = PluginResolver.from(resolversConfig.plugins)
    val resourceProvider = ResourceProvider.from(resolversConfig.resourceProvider)
    val intelliJDependencyProvider = new IntelliJDependencyProvider(intelliJResolver, resourceProvider)
    val pluginDependencyProvider = new PluginDependencyProvider(pluginResolver, resourceProvider)
    val dependencyProvider = new DependencyProvider(intelliJDependencyProvider, pluginDependencyProvider)
    new IntelliJFactory(dependencyProvider, driverConfig)
  }
}
