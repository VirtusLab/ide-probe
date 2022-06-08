package org.virtuslab.ideprobe.scala

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project

import java.lang.reflect.{Field, Method}
import java.util
import scala.annotation.tailrec
//import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
//import org.jetbrains.sbt.settings.SbtSettings
import org.virtuslab.ideprobe.RunConfigurationTransformer

class ScalaTestRunConfigurationTransformer extends RunConfigurationTransformer {


  @tailrec
  final def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
    try cl.getDeclaredMethod(name, parameters: _*)
    catch {
      case e: NoSuchMethodException =>
        if (cl.getSuperclass == null) throw e
        else getMethod(cl.getSuperclass, name, parameters: _*)
    }
  }


  @tailrec
  final def getField(cl: Class[_], name: String): Field = {
    try cl.getField(name)
    catch {
      case e: NoSuchFieldException =>
        if (cl.getSuperclass == null) throw e
        else getField(cl.getSuperclass, name)
    }
  }

  override def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings = {
    val scalaTestRunConfigurationClass = this.getClass.getClassLoader.loadClass("org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration")
    runConfiguration.getConfiguration match {
      case scalaConfig if scalaConfig.getClass == scalaTestRunConfigurationClass =>
        val useSbt = hasSbt(scalaConfig.getProject).asInstanceOf[java.lang.Boolean]
        val testConfigurationDataField = getField(scalaConfig.getClass, "testConfigurationData")
        val testConfigurationData = testConfigurationDataField.get(scalaConfig)
        getMethod(testConfigurationData.getClass, "setUseSbt").invoke(testConfigurationData, useSbt)
        getMethod(testConfigurationData.getClass, "setUseUiWithSbt").invoke(testConfigurationData, useSbt)
      case _ =>
    }
    runConfiguration
  }

  private def hasSbt(project: Project): Boolean = {
    val scalaTestRunConfigurationClass = this.getClass.getClassLoader.loadClass("org.jetbrains.sbt.settings.SbtSettings")
    val sbtSettings = getMethod(scalaTestRunConfigurationClass.getClass, "getInstance", classOf[Project]).invoke(scalaTestRunConfigurationClass, project)
    val getLinkedProjectsSettingsMethod = getMethod(sbtSettings.getClass, "getLinkedProjectsSettings")
    val sbtSettingsEmpty = sbtSettings == null || getLinkedProjectsSettingsMethod.invoke(sbtSettings).asInstanceOf[util.Collection[_]].isEmpty
    !sbtSettingsEmpty
  }
}
