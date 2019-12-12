package com.virtuslab.ideprobe.dependencies

import com.virtuslab.ideprobe.BuildInfo

object BundledDependencies {
  val probePlugin: Plugin = bundle("ideprobe")

  def bundle(name: String): Plugin = Plugin.Bundled(s"$name-${BuildInfo.version}.zip")
}
