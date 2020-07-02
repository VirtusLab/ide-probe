package org.virtuslab.handlers

import com.intellij.ide.plugins.PluginManagerCore
import org.virtuslab.ideprobe.protocol.InstalledPlugin
import org.virtuslab.ideprobe.Extensions._

object Plugins {
  def list: Seq[InstalledPlugin] = {
    PluginManagerCore.getLoadedPlugins.asScala
      .map(plugin => InstalledPlugin(plugin.getPluginId.getIdString, plugin.getVersion))
      .toSeq
  }
}
