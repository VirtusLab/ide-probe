package org.virtuslab.ideprobe.config

import com.google.gson.Gson

import java.nio.file.{CopyOption, FileVisitResult, Files, Path, SimpleFileVisitor}
import org.virtuslab.ideprobe.ide.intellij.{InstalledIntelliJ, IntelliJFactory, LocalIntelliJ}
import org.virtuslab.ideprobe.IdeProbePaths
import org.virtuslab.ideprobe.dependencies.{IntelliJVersion, Plugin}

import java.nio.file.attribute.BasicFileAttributes
import scala.io.Source

trait IntelliJProvider {
  def factory: IntelliJFactory
  def setup(): InstalledIntelliJ
  def version: IntelliJVersion
  def withFactory(factory: IntelliJFactory): IntelliJProvider
}

case class ExistingIntelliJ(
    path: Path,
    plugins: Seq[Plugin],
    factory: IntelliJFactory
) extends IntelliJProvider {
  override def setup(): InstalledIntelliJ = {
    val pluginsDir = path.resolve("plugins")
    val backupDir = Files.createTempDirectory(path, "plugins")
    copyFolder(pluginsDir, backupDir)
    factory.installPlugins(plugins, path)

    new LocalIntelliJ(path, factory.paths, factory.config, backupDir)
  }

  private def copyFolder(source: Path, target: Path): Unit = {
    Files.walkFileTree(
      source,
      new SimpleFileVisitor[Path] {

        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.createDirectories(target.resolve(source.relativize(dir)))
          FileVisitResult.CONTINUE
        }

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.copy(file, target.resolve(source.relativize(file)))
          FileVisitResult.CONTINUE
        }
      }
    )
  }

  override lazy val version: IntelliJVersion = {
    case class ProductInfo(version: String, buildNumber: String)
    val productInfo = path.resolve("product-info.json").toFile
    val source = Source.fromFile(productInfo)
    val content = source.mkString
    source.close
    val gson = new Gson
    val ProductInfo(version, buildNumber) = gson.fromJson(content, classOf[ProductInfo])

    IntelliJVersion(version, Some(buildNumber))
  }

  override def withFactory(factory: IntelliJFactory): IntelliJProvider = copy(factory = factory)
}

case class DefaultIntelliJ(
    version: IntelliJVersion,
    plugins: Seq[Plugin],
    factory: IntelliJFactory
) extends IntelliJProvider {

  override def setup(): InstalledIntelliJ =
    factory.create(version, plugins)

  override def withFactory(factory: IntelliJFactory): IntelliJProvider = copy(factory = factory)
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
        ExistingIntelliJ(path, plugins, factory)
      case IntellijConfig.Default(version, plugins) =>
        DefaultIntelliJ(version, plugins, factory)
    }
  }
}
