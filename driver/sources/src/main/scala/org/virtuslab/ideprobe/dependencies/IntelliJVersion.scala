package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.BuildInfo

final case class IntelliJVersion(build: String, release: Option[String])

object IntelliJVersion {
  val Latest = IntelliJVersion(BuildInfo.intellijBuild, BuildInfo.intellijVersion)
}
