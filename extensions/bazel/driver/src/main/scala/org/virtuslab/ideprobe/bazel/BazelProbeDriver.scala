package org.virtuslab.ideprobe.bazel

import java.nio.file.Path
import org.virtuslab.ideprobe.ProbeDriver
import org.virtuslab.ideprobe.bazel.protocol.BazelEndpoints

object BazelProbeDriver {
  val pluginId = "org.virtuslab.ideprobe.bazel"

  def apply(driver: ProbeDriver): BazelProbeDriver = driver.as(pluginId, new BazelProbeDriver(_))
}


class BazelProbeDriver(val driver: ProbeDriver) {
  def setupBazelExec(path: Path): Unit = {
    driver.send(BazelEndpoints.SetupBazelExecutable, path)
  }
}
