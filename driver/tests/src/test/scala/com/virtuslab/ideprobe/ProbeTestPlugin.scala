package com.virtuslab.ideprobe

import com.virtuslab.ideprobe.dependencies.BundledDependencies
import com.virtuslab.ideprobe.dependencies.Plugin

object ProbeTestPlugin {
  val id = "virtuslab.ideprobe.driver.test"
  val bundled: Plugin = BundledDependencies.bundle("driver-test-plugin")
}
