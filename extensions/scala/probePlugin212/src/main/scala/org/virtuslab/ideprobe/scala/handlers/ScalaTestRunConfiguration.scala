package org.virtuslab.ideprobe.scala.handlers

import java.util.Collections

import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.configurations.{RunConfiguration => IjRunConfiguration}
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration
import org.virtuslab.ideprobe.handlers.{Modules, RunConfigurations, ScalaReflectionApi, Tests}
import org.virtuslab.ideprobe.protocol.TestsRunResult
import org.virtuslab.ideprobe.scala.protocol.{ScalaTestRunConfiguration => RunConfiguration}

import scala.concurrent.ExecutionContext

object ScalaTestRunConfiguration extends ScalaReflectionApi {

  private final val scalaTestConfigurationTypeClass = "org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType"
  private final val scalaTestRunConfigurationFactoryClass = "org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfigurationFactory"
  private final val testKindClass = "org.jetbrains.plugins.scala.testingSupport.test.TestKind"
  private final val allInPackageTestDataClass = "org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData"
  private final val classTestDataClass = "org.jetbrains.plugins.scala.testingSupport.test.testdata.ClassTestData"
  private final val singleTestDataClass = "org.jetbrains.plugins.scala.testingSupport.test.testdata.SingleTestData"

  private final def getKind(kindName: String): Any = {
    val kindTypeClass = Class.forName(testKindClass)
    val testKind = kindTypeClass.getField(kindName).get(kindTypeClass)
    testKind
  }

  def execute(runConfiguration: RunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val configurationType = method(scalaTestConfigurationTypeClass, "instance")()
    val factory = constructor(scalaTestRunConfigurationFactoryClass)(configurationType)
    val configuration = factory.withScalaReflection.method("createTemplateConfiguration", project)(project).asInstanceOf[AbstractTestRunConfiguration]
    configuration.withScalaReflection.method( "setModule", module).apply(module)

    val testConfigurationData = runConfiguration match {
      case RunConfiguration.Module(_) => {
        val kind = getKind("ALL_IN_PACKAGE")
        configuration.withScalaReflection.method("setTestKind", kind)(kind)
        method(allInPackageTestDataClass, "apply")(configuration, "")
      }
      case RunConfiguration.Package(_, packageName) => {
        val kind = getKind("ALL_IN_PACKAGE")
        configuration.withScalaReflection.method("setTestKind", kind)(kind)
        method(allInPackageTestDataClass, "apply")(configuration, packageName)
      }
      case RunConfiguration.Class(_, className) => {
        val kind = getKind("CLAZZ")
        configuration.withScalaReflection.method("setTestKind", kind)(kind)
        method(classTestDataClass, "apply")(configuration, className)
      }
      case RunConfiguration.Method(_, className, methodName) => {
        val kind = getKind("TEST_NAME")
        configuration.withScalaReflection.method("setTestKind", kind)(kind)
        method(singleTestDataClass, "apply")(configuration, className, methodName)
      }
    }
    configuration.withScalaReflection.method("setBeforeRunTasks", Collections.singletonList(new MakeBeforeRunTask))(Collections.singletonList(new MakeBeforeRunTask))
    testConfigurationData.withScalaReflection.method("setWorkingDirectory", project.getBasePath)(project.getBasePath)
    testConfigurationData.withScalaReflection.method("setUseSbt", true)(true)
    testConfigurationData.withScalaReflection.method("setUseUiWithSbt", true)(true)
    configuration.withScalaReflection.setField("testConfigurationData")(testConfigurationData)

    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration.asInstanceOf[IjRunConfiguration])

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)
    runManager.setSelectedConfiguration(settings)

    Tests.awaitTestResults(project, () => RunConfigurations.launch(project, settings))
  }
}
