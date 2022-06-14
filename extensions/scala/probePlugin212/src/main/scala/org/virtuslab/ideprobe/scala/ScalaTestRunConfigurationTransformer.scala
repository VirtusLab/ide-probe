package org.virtuslab.ideprobe.scala

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project

import java.util
import org.virtuslab.ideprobe.RunConfigurationTransformer
import org.virtuslab.ideprobe.handlers.ScalaReflectionApi

class ScalaTestRunConfigurationTransformer extends RunConfigurationTransformer with ScalaReflectionApi {

  override def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings = {
    val scalaTestRunConfigurationClass = this.getClass.getClassLoader.loadClass("org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration")
    runConfiguration.getConfiguration match {
      case scalaConfig if scalaConfig.getClass == scalaTestRunConfigurationClass =>
        val useSbt = hasSbt(scalaConfig.getProject).asInstanceOf[java.lang.Boolean]
        val testConfigurationData = scalaConfig.withScalaReflection.getField( "testConfigurationData")
        testConfigurationData.withScalaReflection.method("setUseSbt", useSbt).apply(useSbt)
        testConfigurationData.withScalaReflection.method("setUseUiWithSbt", useSbt).apply(useSbt)
      case _ =>
    }
    runConfiguration
  }

  private def hasSbt(project: Project): Boolean = {
    val sbtSettings = method("org.jetbrains.sbt.settings.SbtSettings", "getInstance", project).apply(project)
    val getLinkedProjectsSettingsMethod = sbtSettings.withScalaReflection.method("getLinkedProjectsSettings")
    val sbtSettingsEmpty = sbtSettings == null || getLinkedProjectsSettingsMethod(sbtSettings).asInstanceOf[util.Collection[_]].isEmpty
    !sbtSettingsEmpty
  }
}
