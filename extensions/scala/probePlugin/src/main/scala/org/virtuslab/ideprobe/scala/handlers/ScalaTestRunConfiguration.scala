package org.virtuslab.ideprobe.scala.handlers

import java.util.Collections
import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.configurations.{ConfigurationType, RunConfiguration => IjRunConfiguration}
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
//import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration
//import org.jetbrains.plugins.scala.testingSupport.test.TestKind
import org.virtuslab.ideprobe.handlers.{Modules, RunConfigurations, Tests}
import org.virtuslab.ideprobe.protocol.TestsRunResult
import org.virtuslab.ideprobe.scala.protocol.{ScalaTestRunConfiguration => RunConfiguration}

import java.lang.reflect.{Field, Method}
import java.util.logging.Logger
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

object ScalaTestRunConfiguration {

  private final val logger = Logger.getLogger("ScalaTestRunConfiguration")

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
        logger.severe(s"field:$name not found in $cl. Fields:[${cl.getFields.mkString(",")}], Methods:[${cl.getMethods.mkString(",")}]")
        if (cl.getSuperclass == null) throw e
        else getField(cl.getSuperclass, name)
    }
  }

  final def getKind(kindName: String): (Class[_], AnyRef) = {
    import scala.reflect.runtime.{currentMirror => cm}
    val companion = cm.classSymbol(Class.forName("org.jetbrains.plugins.scala.testingSupport.test.TestKind"))
      .companion
    (companion.getClass, companion)
  }

  def execute(runConfiguration: RunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val scalaTestConfigurationTypeClass = Class.forName("org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType")
    val scalaTestRunConfigurationFactoryClass = Class.forName("org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfigurationFactory")
    val abstractTestRunConfigurationClass = Class.forName("org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration")
    val configurationType = getMethod(scalaTestConfigurationTypeClass, "instance").invoke(scalaTestConfigurationTypeClass)
    val factory = scalaTestRunConfigurationFactoryClass.getDeclaredConstructor(classOf[ConfigurationType]).newInstance(configurationType)
    val configuration = getMethod(factory.getClass, "createTemplateConfiguration", classOf[Project]).invoke(factory, project).asInstanceOf[IjRunConfiguration]
    getMethod(configuration.getClass, "setModule", classOf[Module]).invoke(configuration, module)

    runConfiguration match {
      case RunConfiguration.Module(_) => {
        val (kindClass, kind) = getKind("ALL_IN_PACKAGE")
        val setTestKindMethod = getMethod(abstractTestRunConfigurationClass, "setTestKind", kindClass)
        setTestKindMethod.invoke(configuration, kind)
        val testConfigurationData =
          Class.forName("org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData")
            .getDeclaredConstructor(Class.forName("org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration"), classOf[String])
            .newInstance(configuration, "")
        val testConfigurationDataField = getField(abstractTestRunConfigurationClass, "testConfigurationData")
        testConfigurationDataField.set(configuration, testConfigurationData)
        testConfigurationDataField.set(abstractTestRunConfigurationClass, testConfigurationData.asInstanceOf)
      }
      case RunConfiguration.Package(_, packageName) => {
        val (kindClass, kind) = getKind("ALL_IN_PACKAGE")
        val setTestKindMethod = getMethod(abstractTestRunConfigurationClass, "setTestKind", kindClass)
        setTestKindMethod.invoke(configuration, kind)
        val testConfigurationData =
          Class.forName("org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData")
            .getDeclaredConstructor(Class.forName("org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration"), classOf[String])
            .newInstance(configuration, packageName)
        val testConfigurationDataField = getField(abstractTestRunConfigurationClass, "testConfigurationData")
        testConfigurationDataField.set(configuration, testConfigurationData)
        testConfigurationDataField.set(abstractTestRunConfigurationClass, testConfigurationData.asInstanceOf)
      }
      case RunConfiguration.Class(_, className) => {
        val (kindClass, kind) = getKind("ALL_IN_PACKAGE")
        val setTestKindMethod = getMethod(abstractTestRunConfigurationClass, "setTestKind", kindClass)
        val testConfigurationDataField = getField(abstractTestRunConfigurationClass, "testConfigurationData")
        setTestKindMethod.invoke(configuration, kind)
        val testConfigurationData =
          Class.forName("org.jetbrains.plugins.scala.testingSupport.test.testdata.ClassTestData")
            .getDeclaredConstructor(Class.forName("org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration"), classOf[String])
            .newInstance(configuration, className)
        testConfigurationDataField.set(configuration, testConfigurationData)
        testConfigurationDataField.set(abstractTestRunConfigurationClass, testConfigurationData.asInstanceOf)
      }
      case RunConfiguration.Method(_, className, methodName) => {
        val (kindClass, kind) = getKind("ALL_IN_PACKAGE")
        val setTestKindMethod = getMethod(abstractTestRunConfigurationClass, "setTestKind", kindClass)
        val testConfigurationDataField = getField(abstractTestRunConfigurationClass, "testConfigurationData")
        setTestKindMethod.invoke(configuration, kind)
        val testConfigurationData =
          Class.forName("org.jetbrains.plugins.scala.testingSupport.test.testdata.SingleTestData")
            .getDeclaredConstructor(Class.forName("org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration"), classOf[String], classOf[String])
            .newInstance(configuration, className, methodName)
        testConfigurationDataField.set(configuration, testConfigurationData)
        testConfigurationDataField.set(abstractTestRunConfigurationClass, testConfigurationData.asInstanceOf)
      }
    }
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    val testConfigurationData = getField(abstractTestRunConfigurationClass, "testConfigurationData").get(configuration)
    getMethod(testConfigurationData.getClass, "setWorkingDirectory", project.getBasePath.getClass).invoke(testConfigurationData, project.getBasePath)
    getMethod(testConfigurationData.getClass, "setUseSbt", project.getBasePath.getClass).invoke(testConfigurationData, java.lang.Boolean.TRUE)
    getMethod(testConfigurationData.getClass, "setUseUiWithSbt", project.getBasePath.getClass).invoke(testConfigurationData, java.lang.Boolean.TRUE)

    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)
    runManager.setSelectedConfiguration(settings)

    Tests.awaitTestResults(project, () => RunConfigurations.launch(project, settings))
  }
}
