package org.virtuslab.ideprobe.handlers

import com.intellij.ide.plugins.PluginUtil

import org.virtuslab.ideprobe.log.MessageLog
import org.virtuslab.ideprobe.protocol.IdeMessage

object IdeMessages {
  def list: Array[IdeMessage] = {
    MessageLog.all.map { m =>
      val pluginId = m.throwable.flatMap(t => Option(PluginUtil.getInstance.findPluginId(t))).map(_.getIdString)
      val message = m.render
      IdeMessage(m.level, message, pluginId)
    }.toArray
  }
}
