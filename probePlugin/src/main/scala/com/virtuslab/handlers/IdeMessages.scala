package com.virtuslab.handlers

import com.intellij.diagnostic.IdeErrorsDialog
import com.virtuslab.ideprobe.protocol.IdeMessage
import com.virtuslab.log.MessageLog

object IdeMessages {
  def list: Array[IdeMessage] = {
    MessageLog.all.map { m =>
      val pluginId = m.throwable.flatMap(t => Option(IdeErrorsDialog.findPluginId(t))).map(_.getIdString)
      val message = m.render
      IdeMessage(m.level, message, pluginId)
    }.toArray
  }
}
