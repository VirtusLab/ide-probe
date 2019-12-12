package com.virtuslab.ideprobe.ide.intellij

import java.nio.file.Files
import java.nio.file.Path
import com.virtuslab.ideprobe.Extensions._
import com.virtuslab.ideprobe.dependencies.BundledDependencies
import com.virtuslab.ideprobe.dependencies.DependenciesConfig
import com.virtuslab.ideprobe.dependencies.DependencyProvider
import com.virtuslab.ideprobe.dependencies.IntelliJResolver
import com.virtuslab.ideprobe.dependencies.IntelliJVersion
import com.virtuslab.ideprobe.dependencies.Plugin
import com.virtuslab.ideprobe.dependencies.PluginResolver
import com.virtuslab.ideprobe.dependencies.Resource
import com.virtuslab.ideprobe.dependencies.ResourceProvider
import scala.collection.parallel.CollectionConverters._

final class IntelliJFactory(dependencies: DependencyProvider, val config: DriverConfig) {
  def withConfig(config: DriverConfig): IntelliJFactory = new IntelliJFactory(dependencies, config)

  def create(version: IntelliJVersion, plugins: Seq[Plugin]): InstalledIntelliJ = {
    val root = Files.createTempDirectory(s"intellij-instance-${version.build}-")

    installIntelliJ(version, root)
    installPlugins(BundledDependencies.probePlugin +: plugins, root)

    new InstalledIntelliJ(root, config)
  }

  private def installIntelliJ(version: IntelliJVersion, root: Path): Unit = {
    println(s"Installing $version")
    val file = dependencies.fetch(version)
    unpackTo(file, root)
    root.resolve("bin/linux/fsnotifier64").makeExecutable()
    println(s"Installed $version")
  }

  private def installPlugins(plugins: Seq[Plugin], root: Path): Unit = {
    val targetDir = root.resolve("plugins")
    plugins.par.foreach { plugin =>
      val file = dependencies.fetch(plugin)
      unpackTo(file, targetDir)
      println(s"Installed $plugin")
    }
  }

  private def unpackTo(resource: Resource, targetDir: Path): Unit = {
    resource match {
      case Resource.Archive(archive) =>
        archive.extractTo(targetDir)
      case _ =>
        throw new IllegalStateException(s"Not an archive: $resource")
    }
  }
}

object IntelliJFactory {
  val Default =
    new IntelliJFactory(
      new DependencyProvider(IntelliJResolver.Official, PluginResolver.Official, ResourceProvider.Default),
      DriverConfig()
    )

  def from(resolversConfig: DependenciesConfig.Resolvers, driverConfig: DriverConfig): IntelliJFactory = {
    val intelliJResolver = IntelliJResolver.from(resolversConfig.intellij)
    val pluginResolver = PluginResolver.from(resolversConfig.plugins)
    val resourceProvider = ResourceProvider.from(resolversConfig.resourceProvider)
    val dependencyProvider = new DependencyProvider(intelliJResolver, pluginResolver, resourceProvider)
    new IntelliJFactory(dependencyProvider, driverConfig)
  }
}
