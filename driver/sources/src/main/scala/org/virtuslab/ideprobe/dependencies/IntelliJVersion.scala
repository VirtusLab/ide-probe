package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.BuildInfo

final case class IntelliJVersion(build: String, release: Option[String]) {
  def major: Option[String] = {
    release.map(_.split("\\.").take(2).mkString("."))
  }

  def inferredMajor: String = {
    major.getOrElse {
      val firstDigits = build.split('.')(0)
      val year = firstDigits.take(2)
      val version = firstDigits.drop(2)
      s"20$year.$version"
    }
  }
}

object IntelliJVersion {
  val Latest = IntelliJVersion(BuildInfo.intellijBuild, BuildInfo.intellijVersion)
}
