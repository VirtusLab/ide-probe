package org.virtuslab.ideprobe.scala.protocol

import java.nio.file.Path

import pureconfig.generic.auto._

import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method.Request
import org.virtuslab.ideprobe.jsonrpc.PayloadJsonFormat._
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.TestsRunResult

object ScalaEndpoints {
  val GetSbtProjectSettings =
    Request[ProjectRef, SbtProjectSettings]("sbt/project/settings/get")

  val ChangeSbtProjectSettings =
    Request[(ProjectRef, SbtProjectSettingsChangeRequest), Unit]("sbt/project/settings/change")

  val ImportBspProject =
    Request[Path, Unit]("bsp/import")

  val RunScalaTest = Request[ScalaTestRunConfiguration, TestsRunResult]("run/scalatest")
}
