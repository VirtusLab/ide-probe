package org.virtuslab.ideprobe.bazel.protocol

import java.nio.file.Path

import pureconfig.generic.auto._

import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method.Request
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.TestsRunResult

object BazelEndpoints extends ConfigFormat {
  val SetupBazelExecutable = Request[Path, Unit]("bazel/settings/executable/set")
  val RunTestCommand = Request[(BazelCommandParams, ProjectRef), TestsRunResult]("bazel/run/test")
}
