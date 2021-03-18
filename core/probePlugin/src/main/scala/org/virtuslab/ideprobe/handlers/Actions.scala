package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.actionSystem.{
  ActionManager,
  ActionPlaces,
  AnAction,
  AnActionEvent,
  CommonDataKeys,
  DataContext
}
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.openapi.vfs.VirtualFileManager
import java.awt.event.InputEvent
import java.nio.file.Path
import org.virtuslab.ideprobe.handlers.Projects.resolve
import org.virtuslab.ideprobe.protocol.ProjectRef

object Actions extends IntelliJApi {

  def invokeAsync(id: String): Unit = runOnUIAsync {
    invoke(id)

  }

  def invokeSync(id: String): Unit = runOnUISync {
    invoke(id)
  }

  private def getAction(id: String) = {
    val action = ActionManager.getInstance.getAction(id)
    if (action == null) error(s"Action $id not found")
    action
  }

  private def invoke(id: String): Unit = {
    val action = getAction(id)
    val inputEvent = ActionCommand.getInputEvent(id)
    val event = createDummyEvent(action, inputEvent)
    action.actionPerformed(event)
  }

  private def createDummyEvent(action: AnAction, inputEvent: InputEvent): AnActionEvent = {
    val context = new DataContext {
      override def getData(dataId: String): AnyRef = {
        if (dataId == CommonDataKeys.PROJECT.getName) {
          // TODO allow specifying project
          ProjectManager.getInstance.getOpenProjects.headOption.orNull
        } else null
      }
    }
    AnActionEvent.createFromAnAction(action, inputEvent, ActionPlaces.ACTION_SEARCH, context)
  }

  def openFile(projectRef: ProjectRef, file: Path): Unit =
    runOnUISync {
      val vFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(file)
      val project = resolve(projectRef)
      new OpenFileDescriptor(project, vFile).navigate(true)
    }

  def goToLineColumn(projectRef: ProjectRef, line: Int, column: Int): Unit = {
    runOnUISync {
      val ed = FileEditorManager
        .getInstance(resolve(projectRef))
        .getSelectedTextEditor
      val newPosition = new LogicalPosition(line, column)
      ed.getCaretModel.moveToLogicalPosition(newPosition)
    }
  }

  def openFiles(projectRef: ProjectRef): Seq[String] = runOnUISync {
    FileEditorManager.getInstance(resolve(projectRef)).getOpenFiles.map(_.getPath)
  }
}
