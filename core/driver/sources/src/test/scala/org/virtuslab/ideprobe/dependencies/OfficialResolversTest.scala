package org.virtuslab.ideprobe.dependencies

import java.net.HttpURLConnection

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import org.virtuslab.ideprobe.Config
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.IntellijConfig

@RunWith(classOf[JUnit4])
final class OfficialResolversTest {
  private val defaultProbeConfig = IntelliJFixture.defaultConfig

  private val snapshotRepo = "https://www.jetbrains.com/intellij-repository/snapshots/"
  private val orgPathModuleArtifact = "com/jetbrains/intellij/idea/ideaIC/"
  private val buildSpecificEndingOne = "202.6397.20-EAP-SNAPSHOT/ideaIC-202.6397.20-EAP-SNAPSHOT.zip"
  private val buildSpecificEndingTwo = "223.6646-EAP-CANDIDATE-SNAPSHOT/ideaIC-223.6646-EAP-CANDIDATE-SNAPSHOT.zip"

  @Test
  def resolvesImplicitSnapshotForEAPSnapshot(): Unit = {
    val repo = IntelliJZipResolver.community
    val version = IntelliJVersion(build = "202.6397.20", release = None, ext = "zip")
    val artifact = repo.resolve(version).asInstanceOf[Dependency.Artifact]
    assertTrue(artifact.uri.toString == snapshotRepo + orgPathModuleArtifact + buildSpecificEndingOne)
  }

  @Test
  def resolvesImplicitSnapshotFromConfig(): Unit = {
    val probeConfig = IntelliJFixture.readIdeProbeConfig(
      Config.fromString(
        """ probe.intellij.version {
          |  build = "202.6397.20"
          |  release = null
          |}
          |""".stripMargin
      ),
      "probe"
    )
    val intellijResolvers = IntelliJResolver.fromConfig(probeConfig.resolvers)

    val snapshotResolverOption = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version)
      resolved
        .asInstanceOf[Dependency.Artifact]
        .uri
        .toString == snapshotRepo + orgPathModuleArtifact + buildSpecificEndingOne
    }
    assertTrue(snapshotResolverOption.nonEmpty)
  }

  @Test
  def resolvesImplicitSnapshotForEAPCandidateSnapshot(): Unit = {
    val probeConfig = IntelliJFixture.readIdeProbeConfig(
      Config.fromString(
        """ probe.intellij.version {
          |  build = "223.6646"
          |  release = null
          |}
          |""".stripMargin
      ),
      "probe"
    )
    val intellijResolvers = IntelliJResolver.fromConfig(probeConfig.resolvers)

    val snapshotResolverOption = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version)
      resolved
        .asInstanceOf[Dependency.Artifact]
        .uri
        .toString == snapshotRepo + orgPathModuleArtifact + buildSpecificEndingTwo
    }
    assertTrue(snapshotResolverOption.nonEmpty)
  }

  def resolvesImplicitSnapshotWhenEAPSnapshotSuffixAddedToIntellijVersionBuild(): Unit = {
    val probeConfig = IntelliJFixture.readIdeProbeConfig(
      Config.fromString(
        """ probe.intellij.version {
          |  build = "202.6397.20-EAP-SNAPSHOT"
          |  release = null
          |}
          |""".stripMargin
      ),
      "probe"
    )
    val intellijResolvers = IntelliJResolver.fromConfig(probeConfig.resolvers)

    val snapshotResolverOption = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version)
      resolved
        .asInstanceOf[Dependency.Artifact]
        .uri
        .toString == snapshotRepo + orgPathModuleArtifact + buildSpecificEndingOne
    }
    assertTrue(snapshotResolverOption.nonEmpty)
  }

  def resolvesImplicitSnapshotWhenEAPCandidateSnapshotSuffixAddedToIntellijVersionBuild(): Unit = {
    val probeConfig = IntelliJFixture.readIdeProbeConfig(
      Config.fromString(
        """ probe.intellij.version {
          |  build = "223.6646-EAP-CANDIDATE-SNAPSHOT"
          |  release = null
          |}
          |""".stripMargin
      ),
      "probe"
    )
    val intellijResolvers = IntelliJResolver.fromConfig(probeConfig.resolvers)

    val snapshotResolverOption = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version)
      resolved
        .asInstanceOf[Dependency.Artifact]
        .uri
        .toString == snapshotRepo + orgPathModuleArtifact + buildSpecificEndingTwo
    }
    assertTrue(snapshotResolverOption.nonEmpty)
  }

  @Test
  def resolvesBuildToExistingArtifact(): Unit = {
    val version = IntelliJVersion.Latest
    val uri = IntelliJZipResolver.community.resolve(version)

    verify(uri)
  }

  @Test
  def resolvesBuildToExistingArtifactFromConfig(): Unit = {
    val intellijVersion = IntelliJVersion.Latest
    val officialReleasesRepositoryURL = "https://www.jetbrains.com/intellij-repository/releases/"
    val intellijResolvers = IntelliJResolver.fromConfig(defaultProbeConfig.resolvers)
    val releaseResolver = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(intellijVersion)
      // we should use `officialReleasesRepositoryURL` as the default config uses an official release of intelliJ
      resolved.asInstanceOf[Dependency.Artifact].uri.toString.contains(officialReleasesRepositoryURL)
    }.get
    verify(releaseResolver.resolve(intellijVersion))
  }

  @Test
  def resolvesPluginToExistingArtifact(): Unit = {
    val plugin = Plugin("org.intellij.scala", "2020.2.7")
    val uri = PluginResolver.Official.resolve(plugin)

    verify(uri)
  }

  private def verify(dependency: Dependency): Unit = dependency match {
    case Dependency.Artifact(uri) =>
      uri.toURL.openConnection() match {
        case connection: HttpURLConnection =>
          connection.setRequestMethod("GET")
          assertEquals(connection.getResponseCode, 200)
        case _ =>
          fail(s"Not a http connection to $uri")
      }
    case _ =>
      fail(s"Invalid dependency: $dependency")
  }
}
