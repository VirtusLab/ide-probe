package org.virtuslab.ideprobe

import java.nio.file.Path

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure

import com.typesafe.config.ConfigRenderOptions

import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Handler
import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method
import org.virtuslab.ideprobe.jsonrpc.JsonRpcConnection
import org.virtuslab.ideprobe.jsonrpc.JsonRpcEndpoint
import org.virtuslab.ideprobe.jsonrpc.logging.GroupingLogger
import org.virtuslab.ideprobe.jsonrpc.logging.LoggingConfig
import org.virtuslab.ideprobe.jsonrpc.logging.ProbeCommunicationLogger
import org.virtuslab.ideprobe.protocol._

class ProbeDriver(
    protected val connection: JsonRpcConnection,
    val config: Config
)(implicit protected val ec: ExecutionContext)
    extends JsonRpcEndpoint {

  override protected val logger: ProbeCommunicationLogger = {
    val loggingConfig = config.getOrElse[LoggingConfig]("probe.logging", LoggingConfig())
    new GroupingLogger(loggingConfig)
  }
  override def close(): Unit = {
    super.close()
    logger.close()
  }

  protected val handler: Handler = (_, _) => Failure(new Exception("Receiving requests is not supported"))

  def pid(): Long = send(Endpoints.PID)

  def systemProperties(): Map[String, String] = send(Endpoints.SystemProperties)

  def listOpenProjects(): Seq[ProjectRef] = send(Endpoints.ListOpenProjects)

  def preconfigureJdk(): Unit = send(Endpoints.PreconfigureJdk)

  /**
   * Get list of background task names
   * @return
   */
  def backgroundTasks(): Seq[String] = send(Endpoints.BackgroundTasks)

  /**
   * Forces the probe to wait until the specified notification is issued by the IDE
   */
  def awaitNotification(title: String, duration: Duration = Duration.Inf): IdeNotification = {
    send(Endpoints.AwaitNotification, (title, duration))
  }

  /**
   * Builds the specified files, modules or project
   */
  def build(scope: BuildScope = BuildScope.project): BuildResult = {
    build(BuildParams(scope, rebuild = false))
  }

  /**
   * Closes specified project
   */
  def closeProject(name: ProjectRef = ProjectRef.Default): Unit = send(Endpoints.CloseProject, name)

  /**
   * Invokes the specified actions without waiting for it to finish
   */
  def invokeActionAsync(id: String): Unit = send(Endpoints.InvokeActionAsync, id)

  /**
   * Invokes the specified actions and waits for it to finish
   */
  def invokeAction(id: String, waitLogic: WaitLogic = WaitLogic.Default): Unit = {
    withAwait(waitLogic) {
      send(Endpoints.InvokeAction, id)
    }
  }

  /**
   * Opens specified project
   */
  def openProject(path: Path, waitLogic: WaitLogic = WaitLogic.Default): ProjectRef = {
    awaitForProjectOpen(waitLogic) {
      send(Endpoints.OpenProject, path)
    }
  }

  def refreshAllExternalProjectsAsync(): Unit = {
    send(Endpoints.RefreshAllExternalProjects, ProjectRef.Default)
  }

  def refreshAllExternalProjectsAsync(project: ProjectRef): Unit = {
    send(Endpoints.RefreshAllExternalProjects, project)
  }

  /**
   * Only used for developing ide-probe extensions. IntelliJ APIs return prematurely. To make sure that project is open,
   * one must wait for background tasks to finish. This is a helper method to do just this.
   */
  def awaitForProjectOpen(waitLogic: WaitLogic = WaitLogic.Default)(open: => Unit): ProjectRef = {
    val previouslyOpened = listOpenProjects()
    open
    await(waitLogic)
    val currentlyOpened = listOpenProjects()
    val newlyOpened = (currentlyOpened.toSet -- previouslyOpened.toSet).toSeq
    newlyOpened match {
      case Seq() =>
        error(s"Failed to open project, currently open projects are: $currentlyOpened")
      case Seq(single) =>
        single
      case multiple =>
        error(s"More than one project appeared during opening: $multiple")
    }
  }

  /**
   * Sets the 'project compiler output' in the project settings
   */
  def setCompilerOutput(project: ProjectRef = ProjectRef.Default, path: Path): Unit = {
    send(Endpoints.SetCompilerOutput, (project, path))
  }

  /**
   * Rebuilds the specified files, modules or project
   */
  def rebuild(scope: BuildScope = BuildScope.project): BuildResult = build(BuildParams(scope, rebuild = true))

  /**
   * starts the process of shutting down the IDE
   */
  def shutdown(): Unit = send(Endpoints.Shutdown)

  /**
   * Refreshes the file cache (useful, when those were modified outside of IDE)
   */
  def syncFiles(waitLogic: WaitLogic = WaitLogic.Default): Unit = withAwait(waitLogic) {
    send(Endpoints.SyncFiles)
  }

  /**
   * Finds all the files referenced by the specified file
   */
  def fileReferences(path: Path, project: ProjectRef = ProjectRef.Default): Seq[Reference] = {
    send(Endpoints.FileReferences, FileRef(path, project))
  }

  /**
   * Finds all the files referenced by the specified file
   */
  def fileReferences(fileRef: FileRef): Seq[Reference] = {
    send(Endpoints.FileReferences, fileRef)
  }

  /**
   * Finds all navigable elements matching the specified pattern in the specified project
   */
  def find(query: NavigationQuery): List[NavigationTarget] = {
    send(Endpoints.Find, query)
  }

  /**
   * Returns the list of all errors produced by the IDE
   */
  def errors(): Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isError).toList

  /**
   * Returns the list of all warnings produced by the IDE
   */
  def warnings(): Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isWarn).toList

  /**
   * Returns the list of all messages produced by the IDE
   */
  def messages(): Seq[IdeMessage] = send(Endpoints.Messages).toList

  /**
   * Returns the model of the specified project
   */
  def projectModel(name: ProjectRef = ProjectRef.Default): Project =
    send(Endpoints.ProjectModel, name)

  /**
   * Returns the list of all freezes detected by the IDE
   */
  def freezes: Seq[Freeze] = send(Endpoints.Freezes)

  /**
   * Returns the list of test configuration names that are available for the given scope
   */
  def testConfigurations(testScope: TestScope): Seq[String] =
    send(Endpoints.TestConfigurations, testScope)

  /**
   * Runs the specified test configuration with the first available test runner
   */
  def runTestsFromGenerated(runConfiguration: TestScope): TestsRunResult = {
    runTestsFromGenerated(runConfiguration, runnerToSelect = None, shortenCommandLine = None)
  }

  /**
   * Runs the specified test configuration with a test runner containing provided runnerToSelect substring
   */
  def runTestsFromGenerated(runConfiguration: TestScope, runnerToSelect: String): TestsRunResult = {
    runTestsFromGenerated(runConfiguration, runnerToSelect = Some(runnerToSelect), shortenCommandLine = None)
  }

  /**
   * Runs the specified test configuration with a test runner containing provided `runnerToSelect` substring, or the
   * first available test runner if `runnerToSelect` is `None`
   */
  def runTestsFromGenerated(
      runConfiguration: TestScope,
      runnerToSelect: Option[String],
      shortenCommandLine: Option[ShortenCommandLine]
  ): TestsRunResult = {
    send(Endpoints.RunTestsFromGenerated, (runConfiguration, runnerToSelect, shortenCommandLine))
  }

  /**
   * Runs the specified application configuration
   */
  def runApp(runConfiguration: ApplicationRunConfiguration): ProcessResult = {
    send(Endpoints.RunApp, runConfiguration)
  }

  /**
   * Runs the specified JUnit configuration
   */
  def runJUnit(runConfiguration: TestScope): TestsRunResult = {
    send(Endpoints.RunJUnit, runConfiguration)
  }

  /**
   * Runs the tests that have failed during the previous test run
   */
  def rerunFailedTests(projectRef: ProjectRef = ProjectRef.Default): TestsRunResult = {
    send(Endpoints.RerunFailedTests, projectRef)
  }

  /**
   * Saves the current view of the IDE alongside the automatically captured screenshots with the specified name suffix
   */
  def screenshot(nameSuffix: String = ""): Unit = {
    try {
      send(Endpoints.TakeScreenshot, nameSuffix)
    } catch {
      case e: Exception =>
        println("Failed to take a screenshot.")
        e.printStackTrace()
    }
  }

  /**
   * Returns the sdk of the specified project
   */
  def projectSdk(project: ProjectRef = ProjectRef.Default): Option[Sdk] = {
    send(Endpoints.ProjectSdk, project)
  }

  /**
   * Returns the sdk of the specified module
   */
  def moduleSdk(module: ModuleRef): Option[Sdk] = send(Endpoints.ModuleSdk, module)

  /**
   * Returns the list of VCS roots of the specified project
   */
  def vcsRoots(project: ProjectRef = ProjectRef.Default): Seq[VcsRoot] = {
    send(Endpoints.VcsRoots, project)
  }

  /**
   * Returns the list of all installed plugins
   */
  def plugins: Seq[InstalledPlugin] = send(Endpoints.Plugins).toList

  /**
   * Runs inspection given by fully qualified class name on specified file. Optionally it can also run some or all of
   * the quick fixes
   */
  def runLocalInspection(
      className: String,
      targetFile: FileRef,
      runFixesSpec: RunFixesSpec = RunFixesSpec.None,
      waitLogic: WaitLogic = WaitLogic.Default
  ): InspectionRunResult = withAwait(waitLogic) {
    send(Endpoints.RunLocalInspection, InspectionRunParams(className, targetFile, runFixesSpec))
  }

  def highlightInfos(path: Path, project: ProjectRef = ProjectRef.Default): Seq[HighlightInfo] = {
    send(Endpoints.HighlightInfo, FileRef(path, project))
  }

  /**
   * Expand macro in a given file
   */
  def expandMacro(macroText: String, fileRef: FileRef): String = {
    send(Endpoints.ExpandMacro, ExpandMacroData(fileRef, macroText))
  }

  /**
   * Build artifact
   */
  def buildArtifact(projectRef: ProjectRef, artifactName: String): Unit = {
    send(Endpoints.BuildArtifact, (projectRef, artifactName))
  }

  /**
   * Opens file in editor
   */
  def openEditor(file: Path, project: ProjectRef = ProjectRef.Default): Unit = {
    send(Endpoints.OpenEditor, FileRef(file, project))
  }

  /**
   * Go to specific location in current editor 1-based index
   */
  def goToLineColumn(line: Int, column: Int, projectRef: ProjectRef = ProjectRef.Default): Unit = {
    require(line > 0, "line must be greater than zero: " + line)
    require(column > 0, "line must be greater than zero: " + line)
    send(Endpoints.GoToLineColumn, (projectRef, line, column))
  }

  /**
   * List of open editors
   */
  def listOpenEditors(projectRef: ProjectRef = ProjectRef.Default): Seq[Path] = {
    send(Endpoints.ListOpenEditors, projectRef)
  }

  def addTrustedPath(path: Path): Unit = {
    send(Endpoints.AddTrustedPath, path)
  }

  def ping(): Unit = send(Endpoints.Ping)

  /**
   * Saves the config for the further use
   */
  def setConfig(config: Config): Unit = {
    val stringFromConfig = config.source
      .value()
      .fold(e => error(e.prettyPrint()), value => value.render(ConfigRenderOptions.concise()))
    send(Endpoints.SetConfig, stringFromConfig)
  }

  def withAwait[A](waitLogic: WaitLogic)(block: => A): A = {
    val result = block
    await(waitLogic)
    result
  }

  def await(waitLogic: WaitLogic = WaitLogic.Default): Unit = {
    waitLogic.await(this)
  }

  def as[A](extensionPluginId: String, convert: ProbeDriver => A): A = {
    val isLoaded = plugins.exists(_.id == extensionPluginId)
    if (isLoaded) convert(this)
    else throw new IllegalStateException(s"Extension plugin $extensionPluginId is not loaded")
  }

  def send[R](method: Method[Unit, R]): R = {
    send(method, ())
  }

  def send[T, R](method: Method[T, R], parameters: T): R = {
    Await.result(sendAsync(method, parameters), 2.hours)
  }

  def sendAsync[R](method: Method[Unit, R]): Future[R] = {
    sendAsync(method, ())
  }

  def sendAsync[T, R](method: Method[T, R], parameters: T): Future[R] = {
    sendRequest(method, parameters)
  }

  private def build(params: BuildParams): BuildResult = send(Endpoints.Build, params)
}

object ProbeDriver {
  def start(connection: JsonRpcConnection, config: Config)(implicit
      ec: ExecutionContext
  ): ProbeDriver = {
    import scala.concurrent.Future
    val driver = new ProbeDriver(connection, config)
    Future(driver.listen).onComplete(_ => driver.close())
    driver
  }
}
