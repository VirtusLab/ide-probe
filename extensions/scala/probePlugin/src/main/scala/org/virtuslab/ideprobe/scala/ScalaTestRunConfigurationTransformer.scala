package org.virtuslab.ideprobe.scala

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
import org.jetbrains.sbt.settings.SbtSettings
import org.virtuslab.ideprobe.RunConfigurationTransformer

class ScalaTestRunConfigurationTransformer extends RunConfigurationTransformer {
  override def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings = {
    runConfiguration.getConfiguration match {
      case scalaConfig: ScalaTestRunConfiguration =>
        val useSbt = hasSbt(scalaConfig.getProject)
        scalaConfig.testConfigurationData.setUseSbt(useSbt)
        scalaConfig.testConfigurationData.setUseUiWithSbt(useSbt)
      case _ =>
    }
    runConfiguration
  }

  private def hasSbt(project: Project): Boolean = {
    val sbtSettings = SbtSettings.getInstance(project)
    val sbtSettingsEmpty = sbtSettings == null || sbtSettings.getLinkedProjectsSettings.isEmpty
    !sbtSettingsEmpty
  }
}
