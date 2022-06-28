package org.virtuslab.ideprobe.ide.intellij

import java.nio.file.Path

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.Extensions._

final case class IntelliJPaths(
    root: Path,
    config: Path,
    system: Path,
    plugins: Path,
    logs: Path,
    userPrefs: Path
) {
  val bin: Path = root.resolve("bin")
  val bundledPlugins = root.resolve("plugins")
}

object IntelliJPaths {
  def default(root: Path): IntelliJPaths = {
    IntelliJPaths(
      root = root,
      config = root.createDirectory("config"),
      system = root.createDirectory("system"),
      plugins = root.createDirectory("user-plugins"),
      logs = root.createDirectory("logs"),
      userPrefs = root.createDirectory("prefs")
    )
  }
  def fromExistingInstance(root: Path): IntelliJPaths = {
    val bin = root.resolve("bin")
    val ideaProperties = Config.fromFile(bin.resolve("idea.properties"))

    val configPath = ideaProperties.get[Path]("idea.config.path").getOrElse(root.createDirectory("config"))
    val systemPath = ideaProperties.get[Path]("idea.system.path").getOrElse(root.createDirectory("system"))
    val pluginsPath = ideaProperties.get[Path]("idea.plugins.path").getOrElse(root.createDirectory("plugins"))
    val logsPath = ideaProperties.get[Path]("idea.log.path").getOrElse(root.createDirectory("logs"))
    val userPrefsPath = ideaProperties.get[Path]("java.util.prefs.userRoot").getOrElse(root.createDirectory("prefs"))

    new IntelliJPaths(
      root = root,
      config = configPath,
      system = systemPath,
      plugins = pluginsPath,
      logs = logsPath,
      userPrefs = userPrefsPath
    )
  }
}
