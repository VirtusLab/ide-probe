package org.virtuslab.ideprobe.handlers

import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution._
import com.intellij.execution.actions.{ConfigurationContext, ConfigurationFromContext}
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.{RunManagerImpl, RunnerAndConfigurationSettingsImpl}
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.{CommonDataKeys, DataContext, DataKey}
import com.intellij.openapi.module.{Module => IntelliJModule}
import com.intellij.openapi.project.{DumbService, Project}
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{JavaPsiFacade, PsiClass, PsiElement}

import java.util.Collections
import org.virtuslab.ideprobe.{RunConfigurationTransformer, RunnerSettingsWithProcessOutput, UUIDs}
import org.virtuslab.ideprobe.protocol._

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.concurrent.ExecutionContext
import scala.util.Try

object RunConfigurations extends IntelliJApi {

  private class MapDataContext() extends DataContext {
    private val myMap = new java.util.HashMap[String, AnyRef]

    override def getData[T](dataId: DataKey[T]): T = myMap.get(dataId).asInstanceOf[T]

    override def getData(dataId: String): AnyRef = myMap.get(dataId)

    def put[T](dataKey: DataKey[T], data: T): Unit = myMap.put(dataKey.getName, data.asInstanceOf[AnyRef])
  }

  def launch(project: Project, configuration: RunnerAndConfigurationSettings): Unit = {
    val transformedConfiguration = RunConfigurationTransformer.transform(configuration)

    val environment = ExecutionUtil
      .createEnvironment(new DefaultRunExecutor, transformedConfiguration)
      .activeTarget()
      .build()

    ExecutionManager.getInstance(project).restartRunProfile(environment)
  }

  def testConfigurations(
      scope: TestScope
  )(implicit ec: ExecutionContext): Seq[String] = {
    val configurations = availableRunConfigurations(scope)
    configurations.map(_.getConfigurationSettings.getName)
  }

  def runTestsFromGenerated(
      scope: TestScope,
      runnerToSelect: Option[String]
  )(implicit ec: ExecutionContext): TestsRunResult = {
    val configurations = availableRunConfigurations(scope)

    val module = Modules.resolve(scope.module)
    val project = module.getProject
    val producer = runnerToSelect match {
      case Some(fragment) =>
        configurations
          .find(_.toString.contains(fragment))
          .getOrElse(
            error(
              s"Runner name fragment $fragment does not match any configuration name. Available configurations: $configurations."
            )
          )
      case _ =>
        configurations.headOption.getOrElse(error("No test configuration available for specified settings."))
    }
    val selectedConfiguration = producer.getConfigurationSettings

    Tests.awaitTestResults(
      project,
      () => launch(project, selectedConfiguration)
    )
  }

  def rerunFailedTests(projectRef: ProjectRef)(implicit ec: ExecutionContext): TestsRunResult = {
    val project = Projects.resolve(projectRef)
    Tests.awaitTestResults(
      project,
      () => Actions.invokeSync("RerunFailedTests")
    )
  }

  def runJUnit(scope: TestScope)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(scope.module)
    val project = module.getProject

    val configuration = new JUnitConfiguration(UUIDs.randomUUID(), project)
    configuration.setModule(module)
    val data = configuration.getPersistentData
    scope match {
      case TestScope.Module(_) =>
        data.PACKAGE_NAME = ""
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case TestScope.Directory(_, directoryName) =>
        data.setDirName(directoryName)
        data.TEST_OBJECT = JUnitConfiguration.TEST_DIRECTORY
      case TestScope.Package(_, packageName) =>
        data.PACKAGE_NAME = packageName
        data.TEST_OBJECT = JUnitConfiguration.TEST_PACKAGE
      case TestScope.Class(_, className) =>
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
      case TestScope.Method(_, className, methodName) =>
        data.METHOD_NAME = methodName
        data.MAIN_CLASS_NAME = className
        data.TEST_OBJECT = JUnitConfiguration.TEST_METHOD
    }
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)
    RunManager.getInstance(project).addConfiguration(settings)

    Tests.awaitTestResults(project, () => launch(project, settings))
  }

  def runApp(runConfiguration: ApplicationRunConfiguration)(implicit ec: ExecutionContext): ProcessResult = {
    val configuration = registerObservableConfiguration(runConfiguration)
    val project = Projects.resolve(runConfiguration.module.project)

    launch(project, configuration)
    await(configuration.processResult())
  }

  private def registerObservableConfiguration(
      appRunConfig: ApplicationRunConfiguration
  ) = {
    val module = Modules.resolve(appRunConfig.module)
    val project = module.getProject

    val configuration = {
      val configuration = new ApplicationConfiguration(UUIDs.randomUUID(), project)
      configuration.setModule(module)
      configuration.setMainClassName(appRunConfig.mainClass)
      if (appRunConfig.args.nonEmpty) {
        configuration.setProgramParameters(appRunConfig.args.mkString(" "))
      }
      configuration
    }

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)
    RunManager.getInstance(project).setTemporaryConfiguration(settings)

    new RunnerSettingsWithProcessOutput(settings)
  }

  private def availableRunConfigurations(scope: TestScope): Seq[ConfigurationFromContext] = {
    val module = Modules.resolve(scope.module)
    val project = module.getProject

    val dataContext = new MapDataContext
    dataContext.put(CommonDataKeys.PROJECT, project)
    val projectFileDirectory: Class[_] =
      Try(Class.forName("com.intellij.openapi.actionSystem.PlatformCoreDataKeys"))
        .recover{case e: ClassNotFoundException => Class.forName("com.intellij.openapi.actionSystem.PlatformDataKeys")}
        .get
    dataContext.put(projectFileDirectory.getField("MODULE").asInstanceOf[DataKey[Any]], module)

    val psiElement: PsiElement = selectPsiElement(scope, module, project)

    val location = read { PsiLocation.fromPsiElement(psiElement) }
    dataContext.put(Location.DATA_KEY, location)

    val configurationContext = ConfigurationContext.getFromContext(dataContext)
    val runManager = configurationContext.getRunManager.asInstanceOf[RunManagerEx]
    val configurationFromContext = read { configurationContext.getConfiguration }

    runManager.setTemporaryConfiguration(configurationFromContext)
    runManager.setSelectedConfiguration(configurationFromContext)

    waitForSmartMode(project)
    val configurations = read { configurationContext.getConfigurationsFromContext }
    configurations.toSeq
  }

  private def selectPsiElement(scope: TestScope, module: IntelliJModule, project: Project) = {
    scope match {
      case TestScope.Module(_) =>
        val psiDirectory = PSI.findDirectory(project, Modules.contentRoot(module))
        psiDirectory.getOrElse(error(s"Directory of module ${module.getName} not found"))
      case TestScope.Directory(_, directory) =>
        val contentRoot = Modules.contentRoot(module)
        val virtualDirectory = directory.split("[/\\\\]").foldLeft(contentRoot)(_.findChild(_))
        val psiDirectory = PSI.findDirectory(project, virtualDirectory)
        psiDirectory.getOrElse(error(s"Directory $directory not found"))
      case TestScope.Package(_, packageName) =>
        val psiPackage = PSI.findPackage(project, packageName)
        psiPackage.getOrElse(error(s"Package $packageName not found"))
      case TestScope.Class(_, className) =>
        findPsiClass(className, module)
      case TestScope.Method(_, className, methodName) =>
        val psiClass = findPsiClass(className, module)
        val psiMethods = read {
          psiClass.getMethods
        }
        psiMethods
          .find(_.getName == methodName)
          .getOrElse(
            error(
              s"Method $methodName not found in class $className. Available methods: ${psiMethods.map(_.getName)}"
            )
          )
    }
  }

  private def findPsiClass(qualifiedName: String, module: IntelliJModule): PsiClass = {
    val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
    waitForSmartMode(module.getProject)
    val cls = read { JavaPsiFacade.getInstance(module.getProject).findClass(qualifiedName, scope) }
    Option(cls).getOrElse(error(s"Class $qualifiedName not found"))
  }

  private def waitForSmartMode(project: Project) = {
    DumbService.getInstance(project).waitForSmartMode()
  }
}
