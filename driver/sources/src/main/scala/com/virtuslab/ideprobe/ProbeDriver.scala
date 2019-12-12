package com.virtuslab.ideprobe

import java.nio.file.Path

import com.virtuslab.ideprobe.jsonrpc.JsonRpc
import com.virtuslab.ideprobe.jsonrpc.JsonRpc.Method
import com.virtuslab.ideprobe.jsonrpc.JsonRpcConnection
import com.virtuslab.ideprobe.jsonrpc.JsonRpcEndpoint
import com.virtuslab.ideprobe.protocol.BuildParams
import com.virtuslab.ideprobe.protocol.BuildResult
import com.virtuslab.ideprobe.protocol.BuildScope
import com.virtuslab.ideprobe.protocol.Endpoints
import com.virtuslab.ideprobe.protocol.FileRef
import com.virtuslab.ideprobe.protocol.Freeze
import com.virtuslab.ideprobe.protocol.IdeMessage
import com.virtuslab.ideprobe.protocol.IdeNotification
import com.virtuslab.ideprobe.protocol.InstalledPlugin
import com.virtuslab.ideprobe.protocol.ModuleRef
import com.virtuslab.ideprobe.protocol.NavigationQuery
import com.virtuslab.ideprobe.protocol.NavigationTarget
import com.virtuslab.ideprobe.protocol.ProcessResult
import com.virtuslab.ideprobe.protocol.Project
import com.virtuslab.ideprobe.protocol.ProjectRef
import com.virtuslab.ideprobe.protocol.Reference
import com.virtuslab.ideprobe.protocol.ApplicationRunConfiguration
import com.virtuslab.ideprobe.protocol.JUnitRunConfiguration
import com.virtuslab.ideprobe.protocol.TestsRunResult

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.Failure

final class ProbeDriver(protected val connection: JsonRpcConnection)(implicit protected val ec: ExecutionContext)
    extends JsonRpcEndpoint {
  protected val handler: JsonRpc.Handler = (_, _) => Failure(new Exception("Receiving requests is not supported"))

  def pid(): Long = send(Endpoints.PID)

  def listOpenProjects(): Seq[ProjectRef] = send(Endpoints.ListOpenProjects)

  def openProject(path: Path): ProjectRef = send(Endpoints.OpenProject, path)

  def openProjectWithName(path: Path, expectedName: String): ProjectRef = {
    val expectedRef = ProjectRef(expectedName)
    val projectRef = openProject(path)

    @tailrec def attempt(attempts: Int): ProjectRef = {
      awaitIdle()
      val open = listOpenProjects()
      if (open.contains(expectedRef)) {
        expectedRef
      } else {
        if (attempts == 0) {
          throw new RuntimeException(s"Failed to open project $expectedName, open projects are: ${open.mkString("[", ", ", "]")}")
        } else {
          attempt(attempts - 1)
        }
      }
    }

    if (projectRef == expectedRef) {
      projectRef
    } else {
      attempt(attempts = 10)
    }
  }

  def openProjectWithModules(path: Path, expectedModules: Set[String]): ProjectRef = {
    val projectRef = openProject(path)
    val modules = projectModel(projectRef).modules.map(_.name).toSet

    @tailrec def attempt(attempts: Int): ProjectRef = {
      awaitIdle()
      val modules = projectModel(projectRef).modules.map(_.name).toSet
      if (expectedModules.subsetOf(modules)) {
        projectRef
      } else {
        if (attempts == 0) {
          throw new RuntimeException(s"Failed to open project with modules ${expectedModules.mkString("[", ", ", "]")}, " +
            s"loaded modules are: ${modules.mkString("[", ", ", "]")}")
        } else {
          attempt(attempts - 1)
        }
      }
    }

    if (expectedModules.subsetOf(modules)) {
      projectRef
    } else {
      attempt(attempts = 10)
    }
  }

  def closeProject(name: ProjectRef = ProjectRef.Default): Unit = send(Endpoints.CloseProject, name)

  def ping(): Unit = send(Endpoints.Ping)

  def plugins: Seq[InstalledPlugin] = send(Endpoints.Plugins).toList

  def shutdown(): Unit = send(Endpoints.Shutdown)

  def invokeActionAsync(id: String): Unit = send(Endpoints.InvokeActionAsync, id)

  def invokeAction(id: String): Unit = send(Endpoints.InvokeAction, id)

  def fileReferences(project: ProjectRef = ProjectRef.Default, path: String): Seq[Reference] = {
    send(Endpoints.FileReferences, FileRef(project, path))
  }

  def find(query: NavigationQuery): List[NavigationTarget] = {
    send(Endpoints.Find, query)
  }

  def errors: Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isError).toList

  def warnings: Seq[IdeMessage] = send(Endpoints.Messages).filter(_.isWarn).toList

  def messages: Seq[IdeMessage] = send(Endpoints.Messages).toList

  def projectModel(name: ProjectRef = ProjectRef.Default): Project = send(Endpoints.ProjectModel, name)

  def awaitIdle(): Unit = send(Endpoints.AwaitIdle)

  def syncFiles(): Unit = send(Endpoints.SyncFiles)

  def freezes: Seq[Freeze] = send(Endpoints.Freezes)

  def build(scope: BuildScope = BuildScope.project): BuildResult = build(BuildParams(scope, rebuild = false))

  def rebuild(scope: BuildScope = BuildScope.project): BuildResult = build(BuildParams(scope, rebuild = true))

  private def build(params: BuildParams): BuildResult = send(Endpoints.Build, params)

  def awaitNotification(title: String): IdeNotification = send(Endpoints.AwaitNotification, title)

  def run(runConfiguration: ApplicationRunConfiguration): ProcessResult = send(Endpoints.Run, runConfiguration)

  def run(runConfiguration: JUnitRunConfiguration): TestsRunResult = send(Endpoints.RunJUnit, runConfiguration)

  def projectSdk(project: ProjectRef = ProjectRef.Default): Option[String] = send(Endpoints.ProjectSdk, project)

  def moduleSdk(module: ModuleRef): Option[String] = send(Endpoints.ModuleSdk, module)

  def screenshot(nameSuffix: String = ""): Unit = send(Endpoints.TakeScreenshot, nameSuffix)

  def as[A](extensionPluginId: String, convert: ProbeDriver => A): A = {
    val isLoaded = plugins.exists(_.id == extensionPluginId)
    if (isLoaded) convert(this)
    else throw new IllegalStateException(s"Extension plugin $extensionPluginId is not loaded")
  }

  def send[T: ClassTag, R: ClassTag](method: Method[T, R], parameters: T): R = {
    Await.result(sendRequest(method, parameters), 2.hours)
  }

  def send[R: ClassTag](method: Method[Unit, R]): R = {
    send(method, ())
  }
}

object ProbeDriver {
  def start(connection: JsonRpcConnection)(implicit ec: ExecutionContext): ProbeDriver = {
    import scala.concurrent.Future
    val driver = new ProbeDriver(connection)
    Future(driver.listen).onComplete(_ => driver.close())
    driver
  }
}
