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
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.DependenciesConfig

@RunWith(classOf[JUnit4])
class IntelliJResolverTest extends ConfigFormat {
  private val mavenRepo = getClass.getResource(".").toURI.resolve("intellij/maven").toString
  private val mavenArtifact = "artifact"
  private val mavenVersion = IntelliJVersion.snapshot("1.0")

  @Test
  def resolvesWithinCustomRepository(): Unit = {
    val repo = IntelliJZipResolver.fromMaven(mavenRepo, mavenArtifact)

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
        |probe.resolvers.intellij.repositories = [
        |  "$mavenRepo/com/jetbrains/intellij/idea/$mavenArtifact/${mavenVersion.build}/$mavenArtifact-${mavenVersion.build}.zip"
        |]
        |""".stripMargin)
    val intelliJConfig = config[DependenciesConfig.Resolvers]("probe.resolvers")

    val repo = IntelliJZipResolver.fromConfig(intelliJConfig).head
    val artifactUri = repo.resolve(mavenVersion)
    assertExists(artifactUri)
  }

  @Test
  def resolvesIntelliJPatternWithGlobesUsed(): Unit = {
    val probeConfig = IntelliJFixture.readIdeProbeConfig(
      Config.fromString(s"""
                           |probe.resolvers.intellij.repositories = [
                           |  "$mavenRepo/com/*/intellij/*/$mavenArtifact/${mavenVersion.build}/$mavenArtifact-${mavenVersion.build}.zip"
                           |]
                           |""".stripMargin),
      "probe"
    )
    val repo = IntelliJResolver.fromConfig(probeConfig.resolvers).head
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
