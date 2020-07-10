package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.BuildInfo

final case class IntelliJVersion(build: String) extends AnyVal

object IntelliJVersion {
  val Latest = IntelliJVersion(BuildInfo.intellijBuild)
}
