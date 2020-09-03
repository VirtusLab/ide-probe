package org.virtuslab.ideprobe.scala.handlers

import java.util.Collections
import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.RunManager
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.{ScalaTestConfigurationType, ScalaTestRunConfigurationFactory}
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{AllInPackageTestData, ClassTestData, SingleTestData}
import org.jetbrains.plugins.scala.testingSupport.test.{AbstractTestRunConfiguration, TestKind}
import org.virtuslab.ideprobe.handlers.{Modules, RunConfigurationUtil}
import org.virtuslab.ideprobe.protocol.{ScalaTestRunConfiguration, TestsRunResult}
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

    (runConfiguration.packageName, runConfiguration.className, runConfiguration.testName) match {
      case (Some(packageName), None, None) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, packageName)
      }
      case (None, Some(className), None) => {
        configuration.setTestKind(TestKind.CLAZZ)
        configuration.testConfigurationData = ClassTestData(configuration, className)
      }
      case (None, Some(className), Some(testName)) => {
        configuration.setTestKind(TestKind.TEST_NAME)
        configuration.testConfigurationData = SingleTestData(configuration, className, testName)
      }
      case (None, None, None) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, "")
      }
      case _ =>
        throw new RuntimeException(s"Unsupported parameter combination for $runConfiguration")
    }

    configuration.testConfigurationData.setWorkingDirectory(project.getBasePath)
    configuration.testConfigurationData.setUseSbt(true)
    configuration.testConfigurationData.setUseUiWithSbt(true)

    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)

    RunManager.getInstance(project).addConfiguration(settings)
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    RunConfigurationUtil.awaitTestResults(project, () => RunConfigurationUtil.launch(project, settings))
  }
}
