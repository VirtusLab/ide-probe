package org.virtuslab.ideprobe.test

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.messages.MessageDialog
import javax.swing.Icon

class OpenWindowAction extends AnAction("Open Window") with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val p = e.getProject
    val options = Array("opt-1", "opt-2")
    val icon: Icon = null
    val dialog = new MessageDialog("Foo", "Probe Window", options, 0, icon)
    println("Foo: " + dialog.showAndGet())
  }
}
