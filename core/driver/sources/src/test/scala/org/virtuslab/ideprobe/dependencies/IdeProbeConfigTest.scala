package org.virtuslab.ideprobe.dependencies

import scala.concurrent.duration._

import org.junit.Assert._
import org.junit.Test

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.IdeProbeFixture
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.IntellijConfig
import org.virtuslab.ideprobe.config.WorkspaceConfig

class IdeProbeConfigTest extends IdeProbeFixture {
  private val configRoot = "probe" // same as in object IntelliJFixture

  private val defaultIntellijRepositoriesPatterns = Seq(
    "https://download.jetbrains.com/[module]/nightly/[artifact]-[revision][ext]",
    "https://download.jetbrains.com/[module]/[artifact]-[revision][ext]",
    "https://download.jetbrains.com/[module]/nightly/[artifact]-[revision].portable[ext]",
    "https://download.jetbrains.com/[module]/[artifact]-[revision].portable[ext]",
    "https://www.jetbrains.com/intellij-repository/releases/[orgPath]/[module]/[artifact]/[revision]/[artifact]-[revision][ext]",
    "https://www.jetbrains.com/intellij-repository/snapshots/[orgPath]/[module]/[artifact]/[revision]-EAP-SNAPSHOT/[artifact]-[revision]-EAP-SNAPSHOT[ext]"
  )

  private val defaultJbrRepositoriesPatterns = Seq(
    "https://cache-redirector.jetbrains.com/intellij-jbr/jbr_dcevm-[major]-[platform]-x64-b[minor].tar.gz",
    "https://cache-redirector.jetbrains.com/intellij-jbr/jbr-[major]-[platform]-x64-b[minor].tar.gz"
  )

  @Test
  def loadsDefaultValuesFromReferenceConfFile(): Unit = {
    // loading from an empty file - so all configs will be loaded from reference.conf
    val config: Config = Config.fromClasspath("empty.conf")
    val probeConfig = IntelliJFixture.readIdeProbeConfig(config, configRoot)
    // tests for the intellij: IntellijConfig field
    assertTrue(probeConfig.intellij.isInstanceOf[IntellijConfig.Default])
    assertEquals("212.5080.55", probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version.build)
    assertEquals(Some("2021.2.1"), probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version.release)
    assertEquals(Seq.empty, probeConfig.intellij.asInstanceOf[IntellijConfig.Default].plugins)
    // test for the workspace: Option[WorkspaceConfig] field
    assertEquals(None, probeConfig.workspace)
    // tests for the resolvers: DependenciesConfig.Resolvers field
    assertEquals(defaultIntellijRepositoriesPatterns, probeConfig.resolvers.intellij.repositories)
    assertEquals("https://plugins.jetbrains.com/plugin/download", probeConfig.resolvers.plugins.repository.uri)
    assertEquals(defaultJbrRepositoriesPatterns, probeConfig.resolvers.jbr.repositories)
    assertEquals(0, probeConfig.resolvers.retries)
    // tests for the driver: DriverConfig field
    assertEquals(Seq.empty, probeConfig.driver.launch.command)
    assertEquals(30.seconds, probeConfig.driver.launch.timeout)
    assertEquals(false, probeConfig.driver.check.errors.enabled)
    assertEquals(Seq(".*"), probeConfig.driver.check.errors.includeMessages)
    assertEquals(Seq.empty, probeConfig.driver.check.errors.excludeMessages)
    assertEquals(false, probeConfig.driver.check.freezes.enabled)
    assertEquals(1920, probeConfig.driver.xvfb.screen.width)
    assertEquals(1080, probeConfig.driver.xvfb.screen.height)
    assertEquals(24, probeConfig.driver.xvfb.screen.depth)
    assertEquals(false, probeConfig.driver.headless)
    assertEquals(Seq.empty, probeConfig.driver.vmOptions)
    assertEquals(Map.empty, probeConfig.driver.env)
    // tests for the paths: PathsConfig field
    assertEquals(None, probeConfig.paths.base)
    assertEquals(None, probeConfig.paths.instances)
    assertEquals(None, probeConfig.paths.workspaces)
    assertEquals(None, probeConfig.paths.screenshots)
    assertEquals(None, probeConfig.paths.cache)
    assertEquals(None, probeConfig.paths.trusted)
  }

  @Test
  def mergesUserConfigsFromFileWithDefaultConfigs(): Unit = {
    // loading from a non-empty file - so user configs will be merged with configs from reference.conf
    val config: Config = Config.fromClasspath("example.conf")
    val probeConfig = IntelliJFixture.readIdeProbeConfig(config, configRoot)
    // tests for the intellij: IntellijConfig field
    assertTrue(probeConfig.intellij.isInstanceOf[IntellijConfig.Default])
    assertEquals("201.6668.121", probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version.build)
    assertEquals(Some("2021.2.1"), probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version.release)
    assertEquals(1, probeConfig.intellij.plugins.size)
    assertTrue(probeConfig.intellij.plugins.head.isInstanceOf[Plugin.Versioned])
    assertEquals("org.intellij.scala", probeConfig.intellij.plugins.head.asInstanceOf[Plugin.Versioned].id)
    assertEquals("2020.1.27", probeConfig.intellij.plugins.head.asInstanceOf[Plugin.Versioned].version)
    // test for the workspace: Option[WorkspaceConfig] field
    assertTrue(probeConfig.workspace.get.isInstanceOf[WorkspaceConfig.Default])
    assertTrue(
      probeConfig.workspace.get
        .asInstanceOf[WorkspaceConfig.Default]
        .path
        .asInstanceOf[Resource.File]
        .path
        .toString
        .nonEmpty
    )
    // tests for the resolvers: DependenciesConfig.Resolvers field
    assertEquals(
      Seq(
        "https://www.jetbrains.com/intellij-repository/snapshots/" +
          "[orgPath]/[module]/[artifact]/[revision]/[artifact]-[revision].zip"
      ),
      probeConfig.resolvers.intellij.repositories
    )
    assertEquals("https://plugins.jetbrains.com/plugin/download", probeConfig.resolvers.plugins.repository.uri)
    assertEquals(defaultJbrRepositoriesPatterns, probeConfig.resolvers.jbr.repositories)
    assertEquals(0, probeConfig.resolvers.retries)
    // tests for the driver: DriverConfig field
    assertEquals(Seq("idea"), probeConfig.driver.launch.command)
    assertEquals(60.seconds, probeConfig.driver.launch.timeout)
    assertEquals(false, probeConfig.driver.check.errors.enabled)
    assertEquals(Seq(".*"), probeConfig.driver.check.errors.includeMessages)
    assertEquals(Seq.empty, probeConfig.driver.check.errors.excludeMessages)
    assertEquals(false, probeConfig.driver.check.freezes.enabled)
    assertEquals(1920, probeConfig.driver.xvfb.screen.width)
    assertEquals(1080, probeConfig.driver.xvfb.screen.height)
    assertEquals(24, probeConfig.driver.xvfb.screen.depth)
    assertEquals(true, probeConfig.driver.headless)
    assertEquals(Seq.empty, probeConfig.driver.vmOptions)
    assertEquals(Map.empty, probeConfig.driver.env)
    // tests for the paths: PathsConfig field
    assertEquals(None, probeConfig.paths.base)
    assertEquals(None, probeConfig.paths.instances)
    assertEquals(None, probeConfig.paths.workspaces)
    assertEquals(None, probeConfig.paths.screenshots)
    assertEquals(None, probeConfig.paths.cache)
    assertEquals(None, probeConfig.paths.trusted)
  }
}
