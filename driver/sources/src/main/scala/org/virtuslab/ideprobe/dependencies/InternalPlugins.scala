package org.virtuslab.ideprobe.dependencies

import java.net.URI

import org.virtuslab.ideprobe.BuildInfo

object InternalPlugins {
  val probePlugin: Plugin = bundle("ideprobe")

  val robotPlugin: Plugin = {
    val repository = "https://jetbrains.bintray.com/intellij-third-party-dependencies"
    val group = "org.jetbrains.test".replace(".", "/")
    val artifact = "robot-server-plugin"
    val version = BuildInfo.robotVersion
    val uri = s"$repository/$group/$artifact/$version/$artifact-$version.zip"
    Plugin.Direct(new URI(uri))
  }

  val all = Seq(probePlugin, robotPlugin)

  def bundle(name: String): Plugin = Plugin.Bundled(s"$name-${BuildInfo.version}.zip")
}
