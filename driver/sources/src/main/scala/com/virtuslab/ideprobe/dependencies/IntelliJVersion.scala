package com.virtuslab.ideprobe.dependencies

final case class IntelliJVersion(build: String) extends AnyVal

object IntelliJVersion {
  val Latest = IntelliJVersion("202.5792.28-EAP-SNAPSHOT")
}
