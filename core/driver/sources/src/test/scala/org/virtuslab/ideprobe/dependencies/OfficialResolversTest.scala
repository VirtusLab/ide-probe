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
  private val defaultProbeConfig = IntelliJFixture
    .readIdeProbeConfig(Config.fromReferenceConf, "probe")

  @Test
  def resolvesImplicitSnapshot(): Unit = {
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
    val intellijResolvers = IntelliJResolver.fromConfig(probeConfig.resolvers.intellij)
    val snapshotResolverOption = intellijResolvers.find { dependencyResolver =>
      val resolved = dependencyResolver.resolve(probeConfig.intellij.asInstanceOf[IntellijConfig.Default].version)
      resolved.asInstanceOf[Dependency.Artifact].uri.toString.endsWith("-EAP-SNAPSHOT.zip")
    }

    assertTrue(snapshotResolverOption.nonEmpty)
  }

  @Test
  def resolvesBuildToExistingArtifact(): Unit = {
    val intellijVersion = defaultProbeConfig.intellij.asInstanceOf[IntellijConfig.Default].version
    val officialReleasesRepositoryURL = "https://www.jetbrains.com/intellij-repository/releases/"
    val intellijResolvers = IntelliJResolver.fromConfig(defaultProbeConfig.resolvers.intellij)
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
    val uri = PluginResolver.fromConfig(defaultProbeConfig.resolvers.plugins).resolve(plugin)

    verify(uri)
  }

  private def verify(dependency: Dependency): Unit = dependency match {
    case Dependency.Artifact(uri) =>
      uri.toURL.openConnection() match {
        case connection: HttpURLConnection =>
          connection.setRequestMethod("GET")
          assertEquals(200, connection.getResponseCode)
        case _ =>
          fail(s"Not a http connection to $uri")
      }
    case _ =>
      fail(s"Invalid dependency: $dependency")
  }
}
