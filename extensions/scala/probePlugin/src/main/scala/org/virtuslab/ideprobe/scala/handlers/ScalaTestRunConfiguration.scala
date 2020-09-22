package org.virtuslab.ideprobe.scala.handlers

import java.util.Collections

import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.{ScalaTestConfigurationType, ScalaTestRunConfigurationFactory}
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{AllInPackageTestData, ClassTestData, SingleTestData}
import org.jetbrains.plugins.scala.testingSupport.test.{AbstractTestRunConfiguration, TestKind}
import org.virtuslab.ideprobe.handlers.{Modules, RunConfigurationUtil}
import org.virtuslab.ideprobe.protocol.TestsRunResult
import org.virtuslab.ideprobe.scala.protocol.{ScalaTestClassRunConfiguration, ScalaTestMethodRunConfiguration, ScalaTestModuleRunConfiguration, ScalaTestPackageRunConfiguration, ScalaTestRunConfiguration}

import scala.concurrent.ExecutionContext

object ScalaTestRunConfiguration {
  def execute(runConfiguration: ScalaTestRunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val configurationType = ScalaTestConfigurationType.instance
    val factory = new ScalaTestRunConfigurationFactory(configurationType)
    val configuration = factory.createTemplateConfiguration(project).asInstanceOf[AbstractTestRunConfiguration]
    configuration.setModule(module)

    runConfiguration match {
      case ScalaTestModuleRunConfiguration(_) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, "")
      }
      case ScalaTestPackageRunConfiguration(_, packageName) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, packageName)
      }
      case ScalaTestClassRunConfiguration(_, className) => {
        configuration.setTestKind(TestKind.CLAZZ)
        configuration.testConfigurationData = ClassTestData(configuration, className)
      }
      case ScalaTestMethodRunConfiguration(_, className, methodName) => {
        configuration.setTestKind(TestKind.TEST_NAME)
        configuration.testConfigurationData = SingleTestData(configuration, className, methodName)
      }
    }
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    configuration.testConfigurationData.setWorkingDirectory(project.getBasePath)
    configuration.testConfigurationData.setUseSbt(true)
    configuration.testConfigurationData.setUseUiWithSbt(true)

    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)
    runManager.setSelectedConfiguration(settings)

    RunConfigurationUtil.awaitTestResults(project, () => RunConfigurationUtil.launch(project, settings))
  }
}
