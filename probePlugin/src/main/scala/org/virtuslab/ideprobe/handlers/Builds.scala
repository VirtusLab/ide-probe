package org.virtuslab.ideprobe.handlers

import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskManager
import com.intellij.util.messages.MessageBusConnection
import org.virtuslab.ideprobe.protocol.BuildMessage
import org.virtuslab.ideprobe.protocol.BuildParams
import org.virtuslab.ideprobe.protocol.BuildResult
import org.virtuslab.ideprobe.protocol.BuildStepResult

object Builds extends IntelliJApi {
  def build(params: BuildParams): BuildResult = {
    val project = Projects.resolve(params.scope.project)

    val collector = new BuildResultCollector
    val connection = subscribeForBuildResult(project, collector)
    try {
      val result = BackgroundTasks.withAwaitNone {
        val taskManager = ProjectTaskManager.getInstance(project)
        val promise = if (params.scope.files.nonEmpty) {
          buildFiles(params, project, taskManager)
        } else if (params.scope.modules.nonEmpty) {
          buildModules(params, project, taskManager)
        } else {
          buildProject(params, taskManager)
        }
        promise.blockingGet(4, TimeUnit.HOURS)
      }
      collector.buildResult().getOrElse {
        val errorMessages = if(result.hasErrors) Seq(BuildMessage(None, "The build failed but ide-probe could not read error messages")) else Seq.empty

        BuildResult(Seq(BuildStepResult(result.isAborted, errorMessages, Seq.empty, Seq.empty, Seq.empty)))
      }
    } finally {
      connection.disconnect()
    }
  }

  private def buildProject(params: BuildParams, taskManager: ProjectTaskManager) = {
    if (params.rebuild) {
      taskManager.rebuildAllModules()
    } else {
      taskManager.buildAllModules()
    }
  }

  private def buildModules(params: BuildParams, project: Project, taskManager: ProjectTaskManager) = {
    val allModules = ModuleManager.getInstance(project).getModules
    val modulesToBuild = allModules.filter(module => params.scope.modules.contains(module.getName))
    if (params.rebuild) {
      taskManager.rebuild(modulesToBuild: _*)
    } else {
      taskManager.build(modulesToBuild: _*)
    }
  }

  private def buildFiles(params: BuildParams, project: Project, taskManager: ProjectTaskManager) = {
    val root = Paths.get(project.getBasePath)
    val files = params.scope.files.map { file =>
      val path = Paths.get(file)
      val absolute = if (path.isAbsolute) path else root.resolve(path)
      VFS.toVirtualFile(absolute)
    }
    taskManager.compile(files: _*)
  }

  private def subscribeForBuildResult(project: Project, listener: CompilationStatusListener): MessageBusConnection = {
    val connection = project.getMessageBus.connect()
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener)
    connection
  }

  // We might get more than one build result from one build. buildResult is called after
  // there are no background tasks, so all results should already be collected. As we don't
  // know how many builds were started and background task check might be flaky, there is a
  // count down latch that will ensure we collect at least one result in such case.
  class BuildResultCollector extends CompilationStatusListener {
    private var results: Seq[BuildStepResult] = Nil
    private val latch = new CountDownLatch(1)

    def buildResult(): Option[BuildResult] = {
      if (latch.await(30, TimeUnit.SECONDS)) {
        Some(BuildResult(results))
      } else None
    }

    override def compilationFinished(
        aborted: Boolean,
        errors: Int,
        warnings: Int,
        compileContext: CompileContext
    ): Unit = {
      def messages(category: CompilerMessageCategory): Seq[BuildMessage] = {
        compileContext.getMessages(category).map { msg =>
          BuildMessage(Option(msg.getVirtualFile).map(_.toString), msg.getMessage)
        }
      }

      val buildResult = BuildStepResult(
        aborted,
        messages(CompilerMessageCategory.ERROR),
        messages(CompilerMessageCategory.WARNING),
        messages(CompilerMessageCategory.INFORMATION),
        messages(CompilerMessageCategory.STATISTICS)
      )

      results :+= buildResult
      latch.countDown()
    }
  }
}
