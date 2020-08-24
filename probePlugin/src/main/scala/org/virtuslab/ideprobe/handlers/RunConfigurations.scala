package org.virtuslab.ideprobe.handlers

import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.{Collections, UUID}

import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution._
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.testframework.sm.runner.{SMTRunnerEventsAdapter, SMTRunnerEventsListener, SMTestProxy}
import com.intellij.openapi.actionSystem.{CommonDataKeys, LangDataKeys, PlatformDataKeys}
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.{JavaPsiFacade, PsiManager}
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.MapDataContext
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.{ScalaTestConfigurationType, ScalaTestRunConfigurationFactory}
import org.jetbrains.plugins.scala.testingSupport.test.testdata.{AllInPackageTestData, ClassTestData, SingleTestData}
import org.jetbrains.plugins.scala.testingSupport.test.{AbstractTestRunConfiguration, TestKind}
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.RunnerSettingsWithProcessOutput
import org.virtuslab.ideprobe.protocol.{ApplicationRunConfiguration, JUnitRunConfiguration, ProcessResult, TestRun, TestStatus, TestSuite, TestsRunResult, _}

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.concurrent.ExecutionContext

object RunConfigurations extends IntelliJApi {
  def execute(runConfiguration: TestRunConfiguration)(implicit ec: ExecutionContext): TestsRunResult =
    BackgroundTasks.withAwaitNone {
      val project = Projects.resolve(ProjectRef.Default)
      val module = Modules.resolve(runConfiguration.module)

      val dataContext = new MapDataContext
      dataContext.put(CommonDataKeys.PROJECT, project)
      dataContext.put(LangDataKeys.MODULE, module)

      val projectPath = Paths.get(project.getBasePath)

      // TODO getContentRoots returns an array, is taking head only reliable?
      val moduleVirtualFile = ModuleRootManager.getInstance(module).getContentRoots.head
      val moduleDir = runOnUISync {
        PsiManager.getInstance(project).findDirectory(moduleVirtualFile)
      }

      dataContext.put(PlatformDataKeys.SELECTED_ITEMS, List(moduleDir).asJava.toArray)

      val wsVirtualFile = VFS.toVirtualFile(projectPath)
      dataContext.put(PlatformDataKeys.PROJECT_FILE_DIRECTORY, wsVirtualFile)

      val location = PsiLocation.fromPsiElement(moduleDir)
      dataContext.put(Location.DATA_KEY, location)

      val configurationContext = ConfigurationContext.getFromContext(dataContext)
      val runManager = configurationContext.getRunManager.asInstanceOf[RunManagerEx]
      val configurationFromContext = runOnUISync(configurationContext.getConfiguration)

      runManager.setTemporaryConfiguration(configurationFromContext)
      runManager.setSelectedConfiguration(configurationFromContext)

      val configurations = runOnUISync {
        configurationContext.getConfigurationsFromContext
      }
      val producer = runConfiguration.runnerNameFragment match {
        case Some(fragment) =>
          configurations
            .find(_.toString.toLowerCase contains fragment.toLowerCase)
            .getOrElse(
              throw new RuntimeException(
                s"Runner name fragment $fragment does not match any configuration name. Available configurations: $configurations."
              )
            )
        case _ =>
          configurations.headOption.getOrElse(
            throw new RuntimeException(
              "No test configuration available for specified settings."
            )
          )
      }
      val selectedConfiguration = producer.getConfigurationSettings
      val transformedConfiguration = RunConfigurationTransformer.transform(selectedConfiguration)

      awaitTestResults(project, () => launch(project, transformedConfiguration))
    }

  def execute(runConfiguration: JUnitRunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

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

    awaitTestResults(project, settings)
  }

  def execute(runConfiguration: ApplicationRunConfiguration)(implicit ec: ExecutionContext): ProcessResult = {
    val configuration = registerObservableConfiguration(runConfiguration)
    val project = Projects.resolve(runConfiguration.module.project)

    launch(project, configuration)
    await(configuration.processResult())
  }

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

    awaitTestResults(project, settings)
  }

  private def awaitTestResults(project: Project, settings: RunnerAndConfigurationSettingsImpl): TestsRunResult = {
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

    awaitTestResults(project, () => launch(project, settings))
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
    val module = Modules.resolve(mainClass.module)
    val project = module.getProject

    val configuration = {
      val psiClass = {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        DumbService.getInstance(project).waitForSmartMode()
        read { JavaPsiFacade.getInstance(project).findClass(mainClass.mainClass, scope) }
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

  def awaitTestResults(project: Project, launch: () => Unit): TestsRunResult = {
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

    launch()
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
}
