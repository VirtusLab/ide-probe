package org.virtuslab.ideprobe.handlers

import com.intellij.ide.plugins.PluginManagerCore

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.protocol.InstalledPlugin

object Plugins {
  def list: Seq[InstalledPlugin] = {
    PluginManagerCore.getLoadedPlugins.asScala
      .map(plugin => InstalledPlugin(plugin.getPluginId.getIdString, plugin.getVersion))
      .toList
  }
}
