package org.virtuslab.ideprobe.dependencies

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

// the `ext` field (file extension) must be an Option[String] and not a String for the ExistingIntelliJ usage
// inside the `org.virtuslab.ideprobe.dependencies.IntelliJVersionResolver` object
final case class IntelliJVersion(build: String, release: Option[String], ext: Option[String]) {
  def releaseOrBuild: String = release.getOrElse(build)

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

  def compatibleScalaVersion: String =
    if (inferredMajor.toDouble < 2020.3) "2.12" else "2.13"

  override def toString: String = {
    val version = release.fold(build)(r => s"$r, $build")
    s"IntelliJ($version)"
  }
}

object IntelliJVersion {
  implicit val configConvert: ConfigConvert[IntelliJVersion] = deriveConvert[IntelliJVersion]

  // ext = Some(configConvert.map(_.ext).toString) is a convenient way to receive `probe.intellij.version.ext` value
  def snapshot(build: String): IntelliJVersion = {
    IntelliJVersion(build = build, release = None, ext = Some(configConvert.map(_.ext).toString))
  }

  def release(version: String, build: String): IntelliJVersion = {
    IntelliJVersion(build = build, release = Some(version), ext = Some(configConvert.map(_.ext).toString))
  }
}
