package org.virtuslab.ideprobe.scala.handlers

import com.intellij.openapi.project.Project
import org.jetbrains.sbt.project.settings.{SbtProjectSettings => SbtProjectSettingsFromPlugin}

import org.virtuslab.ideprobe.handlers.IntelliJApi
import org.virtuslab.ideprobe.handlers.Projects
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.Setting
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettings
import org.virtuslab.ideprobe.scala.protocol.SbtProjectSettingsChangeRequest

object SbtSettings extends IntelliJApi {
  def getProjectSettings(ref: ProjectRef): SbtProjectSettings = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    SbtProjectSettings(
      useSbtShellForImport = sbtSettings.getUseSbtShellForImport,
      useSbtShellForBuild = sbtSettings.getUseSbtShellForBuild,
      allowSbtVersionOverride = sbtSettings.getAllowSbtVersionOverride
    )
  }

  def changeProjectSettings(ref: ProjectRef, toSet: SbtProjectSettingsChangeRequest): Unit = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    def setSetting[A](setting: Setting[A])(f: (SbtProjectSettingsFromPlugin, A) => Unit): Unit = {
      setting.foreach(value => f(sbtSettings, value))
    }

    setSetting(toSet.useSbtShellForImport)(_.setUseSbtShellForImport(_))
    setSetting(toSet.useSbtShellForBuild)(_.setUseSbtShellForBuild(_))
    setSetting(toSet.allowSbtVersionOverride)(_.setAllowSbtVersionOverride(_))
  }

  private def getSbtSettings(project: Project) =
    SbtProjectSettingsFromPlugin
      .forProject(project)
      .getOrElse(error(s"No settings for ${project.getName}, probably not an sbt project."))
}
