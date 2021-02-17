package org.virtuslab.ideprobe.bazel.protocol

import java.nio.file.Path
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method.Request

object BazelEndpoints extends ConfigFormat {
  val SetupBazelExecutable = Request[Path, Unit]("bazel/settings/executable/set")
}
