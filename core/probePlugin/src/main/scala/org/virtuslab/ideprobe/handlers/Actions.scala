package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.playback.commands.ActionCommand

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
