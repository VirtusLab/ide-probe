package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.dependencies.IntelliJVersion
import org.virtuslab.ideprobe.dependencies.InternalPlugins
import org.virtuslab.ideprobe.dependencies.Plugin

object ProbeTestPlugin {
  val id = "virtuslab.ideprobe.driver.test"
  def bundled(version: IntelliJVersion): Plugin = InternalPlugins.bundleCross("driver-test-plugin", version)
}
