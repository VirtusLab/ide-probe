package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.dependencies.BundledDependencies
import org.virtuslab.ideprobe.dependencies.Plugin

object ProbeTestPlugin {
  val id = "virtuslab.ideprobe.driver.test"
  val bundled: Plugin = BundledDependencies.bundle("driver-test-plugin")
}
