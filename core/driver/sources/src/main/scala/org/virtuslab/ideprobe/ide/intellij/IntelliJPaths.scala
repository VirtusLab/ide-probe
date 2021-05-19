package org.virtuslab.ideprobe.ide.intellij

import org.virtuslab.ideprobe.Config

import java.nio.file.Path
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.config.IdeaProperties.IdeaProperties

final class IntelliJPaths private (
  val root: Path,
  headless: Boolean,
  val config: Path,
  val system: Path,
  val plugins: Path,
  val logs: Path,
  val userPrefs: Path
) {
  val bin: Path = root.resolve("bin")

  val executable: Path = {
    val content = {
      val launcher = bin.resolve("idea.sh").makeExecutable()

      val command =
        if (headless) s"$launcher headless"
        else {
          Display.Mode match {
            case Display.Native => s"$launcher"
            case Display.Xvfb   => s"xvfb-run --server-num=${Display.XvfbDisplayId} $launcher"
          }
        }

      s"""|#!/bin/sh
          |$command "$$@"
          |""".stripMargin
    }

    bin
      .resolve("idea")
      .write(content)
      .makeExecutable()
  }
}

object IntelliJPaths {
  def apply(root: Path, headless: Boolean): IntelliJPaths = {
    val bin = root.resolve("bin")
    val ideaProperties = Config.fromFile(bin.resolve("idea.properties"))
      .getOrElse[IdeaProperties]("", IdeaProperties())

    val configPath = ideaProperties.idea.config.path.getOrElse(root.createDirectory("config"))
    val systemPath = ideaProperties.idea.system.path.getOrElse(root.createDirectory("system"))
    val pluginsPath = ideaProperties.idea.plugins.path.getOrElse(root.createDirectory("plugins"))
    val logsPath = ideaProperties.idea.log.path.getOrElse(root.createDirectory("logs"))
    val userPrefsPath = ideaProperties.java.util.prefs.userRoot.getOrElse {
      val path = root.createDirectory("prefs")
      IntellijPrivacyPolicy.installAgreementIn(path)
      path
    }

    new IntelliJPaths(
      root = root,
      headless = headless,
      config = configPath,
      system = systemPath,
      plugins = pluginsPath,
      logs = logsPath,
      userPrefs = userPrefsPath
    )
  }
}
