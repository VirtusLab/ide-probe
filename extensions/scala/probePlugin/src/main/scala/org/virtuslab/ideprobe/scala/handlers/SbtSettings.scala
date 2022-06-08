package org.virtuslab.ideprobe.scala.handlers

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.project.Project
//import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration
//import org.jetbrains.sbt.project.settings.{SbtProjectSettings => SbtProjectSettingsFromPlugin}
import org.virtuslab.ideprobe.handlers.{BackgroundTasks, IntelliJApi, Projects}
import org.virtuslab.ideprobe.protocol.{ProjectRef, Setting}
import org.virtuslab.ideprobe.scala.handlers.SbtSettings.getMethod
import org.virtuslab.ideprobe.scala.protocol.{SbtProjectSettings, SbtProjectSettingsChangeRequest}

import java.lang.reflect.Method
import scala.annotation.tailrec

object SbtSettings extends IntelliJApi {
  @tailrec
  final def getMethod(cl: Class[_], name: String, parameters: Class[_]*): Method = {
    try cl.getDeclaredMethod(name, parameters: _*)
    catch {
      case e: NoSuchMethodException =>
        if (cl.getSuperclass == null) throw e
        else getMethod(cl.getSuperclass, name, parameters: _*)
    }
  }

  def getProjectSettings(ref: ProjectRef): SbtProjectSettings = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    SbtProjectSettings(
      useSbtShellForImport = getMethod(sbtSettings.getClass, "getUseSbtShellForImport").invoke(sbtSettings).asInstanceOf[Boolean],
      useSbtShellForBuild = getMethod(sbtSettings.getClass, "getUseSbtShellForBuild").invoke(sbtSettings).asInstanceOf[Boolean],
      allowSbtVersionOverride = getMethod(sbtSettings.getClass, "getAllowSbtVersionOverride").invoke(sbtSettings).asInstanceOf[Boolean]
    )
  }

  def changeProjectSettings(ref: ProjectRef, toSet: SbtProjectSettingsChangeRequest): Unit = {
    val project = Projects.resolve(ref)
    val sbtSettings = getSbtSettings(project)

    def setSetting[A](setting: Setting[A])(f: (ExternalProjectSettings, A) => Unit): Unit = {
      setting.foreach(value => f(sbtSettings.asInstanceOf, value))
    }

    setSetting(toSet.useSbtShellForImport){
      (sbtProjectSettings, value) =>
        getMethod(Class.forName("org.jetbrains.sbt.project.settings.SbtProjectSettings"), "setUseSbtShellForImport", classOf[Boolean])
          .invoke(sbtProjectSettings, value.asInstanceOf[java.lang.Boolean])
    }
    setSetting(toSet.useSbtShellForBuild){
      (sbtProjectSettings, value) =>
        getMethod(Class.forName("org.jetbrains.sbt.project.settings.SbtProjectSettings"), "setUseSbtShellForBuild", classOf[Boolean])
          .invoke(sbtProjectSettings, value.asInstanceOf[java.lang.Boolean])
    }
    setSetting(toSet.allowSbtVersionOverride){
      (sbtProjectSettings, value) =>
        getMethod(Class.forName("org.jetbrains.sbt.project.settings.SbtProjectSettings"), "setAllowSbtVersionOverride", classOf[Boolean])
          .invoke(sbtProjectSettings, value.asInstanceOf[java.lang.Boolean])
    }
  }

  private def getSbtSettings[A](project: Project) = {
    import scala.reflect.runtime.{currentMirror => cm}
    val cmp = cm.classSymbol(Class.forName("org.jetbrains.sbt.project.settings.SbtProjectSettings"))
      .companion
    log.error(cmp.getClass.getDeclaredMethods.mkString(","))
      cmp.invoke("forProject")(project.asInstanceOf[Project])
      .asInstanceOf[Option[A]]
      .getOrElse(error(s"No settings for ${project.getName}, probably not an sbt project."))
  }
}
