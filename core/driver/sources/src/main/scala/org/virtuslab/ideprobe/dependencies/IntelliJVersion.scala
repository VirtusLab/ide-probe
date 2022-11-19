package org.virtuslab.ideprobe.dependencies

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.IntellijConfig

final case class IntelliJVersion(build: String, release: Option[String], ext: String) {
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

  private lazy val defaultConfig = IntelliJFixture.defaultConfig.intellij.asInstanceOf[IntellijConfig.Default]

  lazy val Latest: IntelliJVersion = defaultConfig.version

  def snapshot(build: String): IntelliJVersion = {
    IntelliJVersion(build = build, release = None, ext = defaultConfig.version.ext)
  }

  def release(version: String, build: String): IntelliJVersion = {
    IntelliJVersion(build = build, release = Some(version), ext = defaultConfig.version.ext)
  }
}
