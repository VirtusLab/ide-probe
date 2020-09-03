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
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.RunnerSettingsWithProcessOutput
import org.virtuslab.ideprobe.protocol._

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

      RunConfigurationUtil.awaitTestResults(project, () => RunConfigurationUtil.launch(project, transformedConfiguration))
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

    RunConfigurationUtil.awaitTestResults(project, () => RunConfigurationUtil.launch(project, settings))
  }

  def execute(runConfiguration: ApplicationRunConfiguration)(implicit ec: ExecutionContext): ProcessResult = {
    val configuration = registerObservableConfiguration(runConfiguration)
    val project = Projects.resolve(runConfiguration.module.project)

    RunConfigurationUtil.launch(project, configuration)
    await(configuration.processResult())
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
}

object RunConfigurationUtil {
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

  def launch(project: Project, configuration: RunnerAndConfigurationSettings): Unit = {
    val environment = ExecutionUtil
      .createEnvironment(new DefaultRunExecutor, configuration)
      .activeTarget()
      .build()

    ExecutionManager.getInstance(project).restartRunProfile(environment)
  }
}