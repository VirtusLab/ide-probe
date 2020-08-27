package org.virtuslab.ideprobe.scala.protocol

import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method.Request
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.jsonrpc.PayloadJsonFormat._
import pureconfig.generic.auto._

object ScalaEndpoints {
  val GetSbtProjectSettings =
    Request[ProjectRef, SbtProjectSettings]("sbt/project/settings/get")

  val ChangeSbtProjectSettings =
    Request[(ProjectRef, SbtProjectSettingsChangeRequest), Unit]("sbt/project/settings/change")
}
