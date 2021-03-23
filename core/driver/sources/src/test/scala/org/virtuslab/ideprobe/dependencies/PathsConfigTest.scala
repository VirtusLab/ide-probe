package org.virtuslab.ideprobe.dependencies

import java.nio.file.Paths
import java.util.concurrent.Executors

import org.junit.Assert.assertEquals
import org.junit.Test
import org.virtuslab.ideprobe.{Config, IdeProbePaths, IntelliJFixture}

import scala.concurrent.ExecutionContext

class PathsConfigTest {
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  @Test
  def usesProvidedPathsConfig(): Unit = {
    val config = Config.fromString(
      """
        |probe.paths {
        | base = "/base"
        | instances = "/instances"
        | workspaces = "/workspaces"
        | screenshots = "/screenshots"
        | cache = "/cache"
        |}
        |""".stripMargin)
    val fixture = IntelliJFixture.fromConfig(config)

    val expected = IdeProbePaths(
      Paths.get("/base"),
      Paths.get("/instances"),
      Paths.get("/workspaces"),
      Paths.get("/screenshots"),
      Paths.get("/cache")
    )

    assertEquals(expected, fixture.probePaths)
  }

  @Test
  def usesProvidedBasePath(): Unit = {
    val config = Config.fromString(
      """
        |probe.paths {
        | base = "/base"
        |}
        |""".stripMargin)
    val fixture = IntelliJFixture.fromConfig(config)

    val basePath = Paths.get("/base")
    val expected = IdeProbePaths(
      basePath,
      basePath.resolve("instances"),
      basePath.resolve("workspaces"),
      basePath.resolve("screenshots"),
      basePath.resolve("cache")
    )

    assertEquals(expected, fixture.probePaths)
  }

  @Test
  def usesTempAsDefaultBasePath(): Unit = {
    val config = Config.fromString("probe {}")
    val fixture = IntelliJFixture.fromConfig(config)

    val basePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("ide-probe")
    val expected = IdeProbePaths(
      basePath,
      basePath.resolve("instances"),
      basePath.resolve("workspaces"),
      basePath.resolve("screenshots"),
      basePath.resolve("cache")
    )

    assertEquals(expected, fixture.probePaths)
  }
}
