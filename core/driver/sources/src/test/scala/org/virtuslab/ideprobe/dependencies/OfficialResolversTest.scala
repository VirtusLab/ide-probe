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
    val repo = IntelliJZipResolver.community
    val version = IntelliJVersion("202.6397.20", None)

    val artifact = repo.resolve(version).asInstanceOf[Dependency.Artifact]

    assertTrue(artifact.uri.toString.endsWith("-EAP-SNAPSHOT.zip"))
  }

  @Test
  def resolvesBuildToExistingArtifact(): Unit = {
    val version = defaultProbeConfig.intellij.asInstanceOf[IntellijConfig.Default].version
    val uri = IntelliJZipResolver.community.resolve(version)

    verify(uri)
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
          assertEquals(connection.getResponseCode, 200)
        case _ =>
          fail(s"Not a http connection to $uri")
      }
    case _ =>
      fail(s"Invalid dependency: $dependency")
  }
}
