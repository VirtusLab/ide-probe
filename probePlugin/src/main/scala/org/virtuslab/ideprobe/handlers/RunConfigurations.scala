package org.virtuslab.ideprobe.handlers

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
import com.intellij.openapi.actionSystem.{CommonDataKeys, LangDataKeys}
import com.intellij.openapi.module.{Module => IntelliJModule}
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{JavaPsiFacade, PsiClass, PsiElement, PsiManager}
import com.intellij.testFramework.MapDataContext
import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.RunnerSettingsWithProcessOutput
import org.virtuslab.ideprobe.protocol._

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.concurrent.ExecutionContext

object RunConfigurations extends IntelliJApi {
  def execute(testRunnerMatch: TestRunConfigurationMatch)(implicit ec: ExecutionContext): TestsRunResult =
    BackgroundTasks.withAwaitNone {
      val TestRunConfigurationMatch(runConfiguration, runnerNameFragment) = testRunnerMatch
      val project = Projects.resolve(ProjectRef.Default)
      val module = Modules.resolve(runConfiguration.module)

      val dataContext = new MapDataContext
      dataContext.put(CommonDataKeys.PROJECT, project)
      dataContext.put(LangDataKeys.MODULE, module)

      val psiElement: PsiElement = runConfiguration match {
        case TestRunConfiguration.Module(_) => {
          val moduleVirtualFile = ModuleRootManager.getInstance(module).getContentRoots.head
          val psiDirectory = read {
            PsiManager.getInstance(project).findDirectory(moduleVirtualFile)
          }
          Option(psiDirectory).getOrElse(error(s"Directory of module ${module.getName} not found"))
        }
        case TestRunConfiguration.Directory(_, directoryName) => {
          val moduleContentRoots = ModuleRootManager.getInstance(module).getContentRoots.head
          val dirPath = Paths.get(moduleContentRoots.getPath, directoryName.replace(".", "/"))
          val dirVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(dirPath)
          val psiDirectory = read {
            PsiManager.getInstance(project).findDirectory(dirVirtualFile)
          }
          Option(psiDirectory).getOrElse(error(s"Directory $directoryName not found"))
        }
        case TestRunConfiguration.Package(_, packageName) => {
          val psiPackage = new PsiPackageImpl(PsiManager.getInstance(project), packageName)
          Option(psiPackage).getOrElse(error(s"Package $packageName not found"))
        }
        case TestRunConfiguration.Class(_, className) => {
          Option(findPsiClass(className, module)).getOrElse(error(s"Class $className not found"))
        }
        case TestRunConfiguration.Method(_, className, methodName) => {
          val psiClass = findPsiClass(className, module)
          val psiMethods = read {
            psiClass.getMethods
          }
          psiMethods.find(_.getName == methodName)
            .getOrElse(error(s"Method $methodName not found in class $className. Available methods: ${psiMethods.map(_.getName)}"))
        }
      }

      val location = read {
        PsiLocation.fromPsiElement(psiElement)
      }
      dataContext.put(Location.DATA_KEY, location)

      val configurationContext = ConfigurationContext.getFromContext(dataContext)
      val runManager = configurationContext.getRunManager.asInstanceOf[RunManagerEx]
      val configurationFromContext = read {
        configurationContext.getConfiguration
      }

      runManager.setTemporaryConfiguration(configurationFromContext)
      runManager.setSelectedConfiguration(configurationFromContext)

      val configurations = read {
        configurationContext.getConfigurationsFromContext
      }
      val producer = runnerNameFragment match {
        case Some(fragment) =>
          configurations
            .find(_.toString contains fragment)
            .getOrElse(
              error(
                s"Runner name fragment $fragment does not match any configuration name. Available configurations: $configurations."
              )
            )
        case _ =>
          configurations.headOption.getOrElse(
            error(
              "No test configuration available for specified settings."
            )
          )
      }
      val selectedConfiguration = producer.getConfigurationSettings
      val transformedConfiguration = RunConfigurationTransformer.transform(selectedConfiguration)

      RunConfigurationUtil.awaitTestResults(project, () => RunConfigurationUtil.launch(project, transformedConfiguration))
    }

  private def findPsiClass(qualifiedName: String, module: IntelliJModule): PsiClass = {
    val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
    DumbService.getInstance(module.getProject).waitForSmartMode()
    read { JavaPsiFacade.getInstance(module.getProject).findClass(qualifiedName, scope) }
  }

  def execute(runConfiguration: TestRunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

    val configuration = new JUnitConfiguration(UUID.randomUUID().toString, project)
    configuration.setModule(module)
    val data = configuration.getPersistentData
    runConfiguration match {
      case TestRunConfiguration.Module(_) =>
        data.PACKAGE_NAME = ""
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case TestRunConfiguration.Directory(_, directoryName) =>
        data.setDirName(directoryName)
        data.TEST_OBJECT = JUnitConfiguration.TEST_DIRECTORY
      case TestRunConfiguration.Package(_, packageName) =>
        data.PACKAGE_NAME = packageName
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case TestRunConfiguration.Class(_, className) =>
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
      case TestRunConfiguration.Method(_, className, methodName) =>
        data.METHOD_NAME = methodName
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_METHOD
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
      val psiClass = findPsiClass(mainClass.mainClass, module)

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