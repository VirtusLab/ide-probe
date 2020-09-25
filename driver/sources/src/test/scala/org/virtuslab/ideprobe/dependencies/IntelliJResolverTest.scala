package org.virtuslab.ideprobe.dependencies

import java.nio.file.Files
import java.nio.file.Paths

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.config.DependenciesConfig

@RunWith(classOf[JUnit4])
class IntelliJResolverTest extends ConfigFormat {
  private val mavenRepo = getClass.getResource(".").toURI.resolve("intellij/maven").toString
  private val mavenGroup = "group"
  private val mavenArtifact = "artifact"
  private val mavenVersion = IntelliJVersion("1.0", None)

  @Test
  def resolvesWithinCustomRepository(): Unit = {
    val repo = IntelliJZipResolver.fromMaven(mavenRepo, mavenGroup, mavenArtifact)

    val artifactUri = repo.resolve(mavenVersion)

    assertExists(artifactUri)
  }

  @Test
  def createsMavenResolverFromConfig(): Unit = {
    // I'd rather read full IdeProbeConfig than internal case classes
    // to avoid the need for creating ConfigReaders, but this is for sake of this test
    // That doesn't have IdeProbeConfig on classpath.
    import pureconfig.generic.auto._

    val config = Config.fromString(s"""
        |probe.resolvers.intellij.repository {
        |  uri = "$mavenRepo"
        |  group = $mavenGroup
        |  artifact = $mavenArtifact
        |}
        |""".stripMargin)
    val intelliJConfig = config[DependenciesConfig.IntelliJ]("probe.resolvers.intellij")

    val repo = IntelliJZipResolver.from(intelliJConfig)
    val artifactUri = repo.resolve(mavenVersion)
    assertExists(artifactUri)
  }

  private def assertExists(dependency: Dependency): Unit = dependency match {
    case Dependency.Artifact(uri) =>
      assertTrue(s"Resolved invalid artifact: $uri", Files.exists(Paths.get(uri)))
    case _ =>
      fail("Dependency should be resolved to a remote artifact")
  }
}
