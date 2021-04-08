package org.virtuslab.ideprobe.ide.intellij

import com.google.gson.Gson
import java.nio.file.{Files, Path}
import java.util.stream.{Collectors, Stream => JStream}

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.IdeProbePaths
import org.virtuslab.ideprobe.config.{DependenciesConfig, DriverConfig}
import org.virtuslab.ideprobe.dependencies._

import scala.io.Source

final class IntelliJFactory(
  dependencies: DependencyProvider,
  val paths: IdeProbePaths,
  val config: DriverConfig
) {
  def withConfig(config: DriverConfig): IntelliJFactory = new IntelliJFactory(dependencies, paths, config)

  def withPaths(paths: IdeProbePaths): IntelliJFactory = new IntelliJFactory(dependencies, paths, config)

  def create(version: IntelliJVersion, plugins: Seq[Plugin]): InstalledIntelliJ = {
    val root = createInstanceDirectory(version)

    val allPlugins = InternalPlugins.probePluginForIntelliJ(version) +: plugins

    installIntelliJ(version, root)
    installPlugins(allPlugins, root)

    new DownloadedIntelliJ(root, paths, config)
  }

  def createInstanceDirectory(version: IntelliJVersion): Path = {
    val path = paths.instances.createTempDirectory(s"intellij-instance-${version.build}-")

    Files.createDirectories(path)
  }

  def installIntelliJ(version: IntelliJVersion, root: Path): Unit = {
    println(s"Installing $version")
    val file = dependencies.fetch(version)
    toArchive(file).extractTo(root)
    root.resolve("bin/linux/fsnotifier64").makeExecutable()
  }

  // This method differentiates plugins by their root entries in zip
  // assuming that plugins with same root entries are the same plugin
  // and only installs last occurrance of such plugin in the list
  // in case of duplicates.
  def installPlugins(plugins: Seq[Plugin], root: Path): Unit = {
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
        new IntelliJDependencyProvider(Seq(IntelliJZipResolver.Community), ResourceProvider.Default),
        new PluginDependencyProvider(Seq(PluginResolver.Official), ResourceProvider.Default)
      ),
      IdeProbePaths.Default,
      DriverConfig()
    )

  def from(
            resolversConfig: DependenciesConfig.Resolvers,
            paths: IdeProbePaths,
            driverConfig: DriverConfig
  ): IntelliJFactory = {
    val intelliJResolver = IntelliJZipResolver.from(resolversConfig.intellij)
    val pluginResolver = PluginResolver.from(resolversConfig.plugins)
    val resourceProvider = ResourceProvider.from(paths)
    val intelliJDependencyProvider = new IntelliJDependencyProvider(Seq(intelliJResolver), resourceProvider)
    val pluginDependencyProvider = new PluginDependencyProvider(Seq(pluginResolver), resourceProvider)
    val dependencyProvider = new DependencyProvider(intelliJDependencyProvider, pluginDependencyProvider)
    new IntelliJFactory(dependencyProvider, paths, driverConfig)
  }

  def version(root: Path): IntelliJVersion = {
    case class ProductInfo(version: String, buildNumber: String)
    val productInfo = root.resolve("product-info.json").toFile
    val source = Source.fromFile(productInfo)
    val content = source.mkString
    source.close
    val gson = new Gson
    val ProductInfo(version, buildNumber) = gson.fromJson(content, classOf[ProductInfo])

    IntelliJVersion(version, Some(buildNumber))
  }
}
