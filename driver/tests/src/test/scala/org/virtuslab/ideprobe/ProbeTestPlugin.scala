package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.dependencies.InternalPlugins
import org.virtuslab.ideprobe.dependencies.Plugin

object ProbeTestPlugin {
  val id = "virtuslab.ideprobe.driver.test"
  val bundled: Plugin = InternalPlugins.bundle("driver-test-plugin")
}
