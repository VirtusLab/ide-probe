package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.ui.playback.commands.ActionCommand
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import org.virtuslab.ideprobe.handlers.Projects.resolve
import org.virtuslab.ideprobe.protocol.ProjectRef

object Actions extends IntelliJApi {

  def invokeAsync(id: String): Unit = runOnUIAsync {
    invoke(id, now = true)
  }

  def invokeSync(id: String): Unit = runOnUISync {
    invoke(id, now = true)
  }

  private def getAction(id: String) = {
    val action = ActionManager.getInstance.getAction(id)
    if (action == null) error(s"Action $id not found")
    action
  }

  private def invoke(id: String, now: Boolean): Unit = {
    val action = getAction(id)
    val inputEvent = ActionCommand.getInputEvent(id)
    ActionManager.getInstance().tryToExecute(action, inputEvent, null, null, now)
  }

}
