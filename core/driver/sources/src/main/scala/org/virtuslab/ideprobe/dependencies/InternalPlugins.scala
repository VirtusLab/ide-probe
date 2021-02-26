package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.BuildInfo

object InternalPlugins {
  val probePlugin: Plugin = bundle("ideprobe")

  val all = Seq(probePlugin)

  def bundle(name: String): Plugin = Plugin.Bundled(s"$name-${BuildInfo.version}.zip")
}
