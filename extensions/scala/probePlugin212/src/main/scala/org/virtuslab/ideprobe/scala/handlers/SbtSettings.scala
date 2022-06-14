package org.virtuslab.ideprobe.scala.handlers

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.project.Project

import scala.reflect.runtime.universe._
import org.virtuslab.ideprobe.handlers.{IntelliJApi, Projects, ScalaReflectionApi}
import org.virtuslab.ideprobe.protocol.{ProjectRef, Setting}
import org.virtuslab.ideprobe.scala.protocol.{SbtProjectSettings, SbtProjectSettingsChangeRequest}


object SbtSettings extends IntelliJApi with ScalaReflectionApi {

  private final def getInstanceMethod(instance: Any, methodName: String): MethodMirror = {
    import scala.reflect.runtime.universe

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val objReflect = runtimeMirror.reflect(instance)
    val methodSymbol = objReflect.symbol.info.member(universe.TermName(methodName)).alternatives.head.asMethod
    val method = objReflect.reflectMethod(methodSymbol)
    method
  }

  def getProjectSettings(ref: ProjectRef): SbtProjectSettings = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    SbtProjectSettings(
      useSbtShellForImport = sbtSettings.withScalaReflection.method( "getUseSbtShellForImport", sbtSettings)(sbtSettings).asInstanceOf[Boolean],
      useSbtShellForBuild = sbtSettings.withScalaReflection.method( "getUseSbtShellForBuild", sbtSettings)(sbtSettings).asInstanceOf[Boolean],
      allowSbtVersionOverride = sbtSettings.withScalaReflection.method( "getAllowSbtVersionOverride", sbtSettings)(sbtSettings).asInstanceOf[Boolean]
    )
  }

  def changeProjectSettings(ref: ProjectRef, toSet: SbtProjectSettingsChangeRequest): Unit = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    def setSetting(setting: Setting[Boolean])(f: (ExternalProjectSettings, Boolean) => Unit): Unit = {
      setting.foreach(value => f(sbtSettings.asInstanceOf[ExternalProjectSettings], value))
    }

    setSetting(toSet.useSbtShellForImport){
      (sbtProjectSettings, value) =>
        getInstanceMethod(sbtProjectSettings, "setUseSbtShellForImport")
          .apply(value)
    }
    setSetting(toSet.useSbtShellForBuild){
      (sbtProjectSettings, value) =>
        getInstanceMethod(sbtProjectSettings, "setUseSbtShellForBuild")
          .apply(value)
    }
    setSetting(toSet.allowSbtVersionOverride){
      (sbtProjectSettings, value) =>
        getInstanceMethod(sbtProjectSettings, "setAllowSbtVersionOverride")
          .apply(value)
    }
  }

  def getSbtProjectSettings() = {
    import scala.reflect.runtime.universe

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    val module = runtimeMirror.staticModule("org.jetbrains.sbt.project.settings.SbtProjectSettings")
    val obj = runtimeMirror.reflectModule(module)
    obj.instance
  }

  private def getSbtSettings(project: Project) = {
    val sbtSettingsObj = getSbtProjectSettings()
    sbtSettingsObj.getClass.getMethod("forProject", classOf[Project])
      .invoke(sbtSettingsObj, project)
      .asInstanceOf[Option[_]]
      .getOrElse(error(s"No settings for ${project.getName}, probably not an sbt project."))
  }

}
