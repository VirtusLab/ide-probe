package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.BuildInfo
import org.virtuslab.ideprobe.IntelliJFixture

object InternalPlugins {

  def bundleCross(name: String, intelliJVersion: IntelliJVersion): Plugin =
    Plugin.BundledCrossVersion(name, intelliJVersion.compatibleScalaVersion, BuildInfo.version)

  def probePluginForIntelliJ(intelliJVersion: IntelliJVersion): Plugin = bundleCross("ideprobe", intelliJVersion)

  def installCrossVersionPlugin(name: String): IntelliJFixture => IntelliJFixture = { fixture =>
    fixture.withPlugin(bundleCross(name, fixture.version))
  }
}
