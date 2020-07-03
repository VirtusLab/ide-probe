package org.virtuslab.handlers

import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.virtuslab.idea.RunnerSettingsWithProcessOutput
import org.virtuslab.ideprobe.protocol.ApplicationRunConfiguration
import org.virtuslab.ideprobe.protocol.JUnitRunConfiguration
import org.virtuslab.ideprobe.protocol.ProcessResult
import org.virtuslab.ideprobe.protocol.TestRun
import org.virtuslab.ideprobe.protocol.TestStatus
import org.virtuslab.ideprobe.protocol.TestSuite
import org.virtuslab.ideprobe.protocol.TestsRunResult
import scala.concurrent.ExecutionContext
import org.virtuslab.ideprobe.Extensions._

object RunConfigurations extends IntelliJApi {
  def execute(runConfiguration: JUnitRunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val (project, module) = Modules.resolve(runConfiguration.module)

    val configuration = new JUnitConfiguration(UUID.randomUUID().toString, project)
    configuration.setModule(module)
    val data = configuration.getPersistentData
    (runConfiguration.methodName, runConfiguration.mainClass, runConfiguration.packageName, runConfiguration.directory) match {
      case (Some(methodName), Some(className), None, None) =>
        data.METHOD_NAME = methodName
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_METHOD
      case (None, Some(className), None, None) =>
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
      case (None, None, Some(packageName), None) =>
        data.PACKAGE_NAME = packageName
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case (None, None, None, Some(directory)) =>
        data.setDirName(directory)
        data.TEST_OBJECT = JUnitConfiguration.TEST_DIRECTORY
      case (None, None, None, None) =>
        data.PACKAGE_NAME = ""
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case _ =>
        throw new RuntimeException(s"Unsupported parameter combination for $runConfiguration")
    }
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)
    RunManager.getInstance(project).addConfiguration(settings)
    val latch = new CountDownLatch(1)
    var testProxy: SMTestProxy.SMRootTestProxy = null
    project.getMessageBus
      .connect()
      .subscribe(
        SMTRunnerEventsListener.TEST_STATUS,
        new SMTRunnerEventsAdapter() {
          override def onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy): Unit = {
            testProxy = testsRoot
            latch.countDown()
          }
        }
      )
    launch(project, settings)
    latch.await()

    def createSuite(suiteProxy: SMTestProxy) = {
      val tests = suiteProxy.getChildren.asScala.map { testProxy =>
        val status =
          if (testProxy.isPassed) TestStatus.Passed
          else if (testProxy.isIgnored) TestStatus.Ignored
          else TestStatus.Failed(testProxy.getErrorMessage + testProxy.getStacktrace)
        TestRun(testProxy.getPresentableName, testProxy.getDuration, status)
      }.toSeq
      TestSuite(suiteProxy.getPresentableName, tests)
    }

    val suites = if (testProxy.getChildren.asScala.exists(_.isLeaf)) {
      Seq(createSuite(testProxy))
    } else {
      testProxy.getChildren.asScala.map(createSuite).toSeq
    }

    TestsRunResult(suites)
  }

  def execute(runConfiguration: ApplicationRunConfiguration)(implicit ec: ExecutionContext): ProcessResult = {
    val configuration = registerObservableConfiguration(runConfiguration)

    val project = Projects.resolve(runConfiguration.module.project)
    launch(project, configuration)
    await(configuration.processResult())
  }

  private def launch(project: Project, configuration: RunnerAndConfigurationSettings): Unit = {
    val environment = ExecutionUtil
      .createEnvironment(new DefaultRunExecutor, configuration)
      .activeTarget()
      .build()

    ExecutionManager.getInstance(project).restartRunProfile(environment)
  }

  private def registerObservableConfiguration(
      mainClass: ApplicationRunConfiguration
  ): RunnerSettingsWithProcessOutput = {
    val (project, module) = Modules.resolve(mainClass.module)

    val configuration = {
      val psiClass = {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        DumbService.getInstance(project).waitForSmartMode()
        JavaPsiFacade.getInstance(project).findClass(mainClass.mainClass, scope)
      }

      val name = UUID.randomUUID()
      val configuration = new ApplicationConfiguration(name.toString, project)
      configuration.setMainClass(psiClass)
      configuration
    }

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)
    RunManager.getInstance(project).addConfiguration(settings)

    new RunnerSettingsWithProcessOutput(settings)
  }

}
