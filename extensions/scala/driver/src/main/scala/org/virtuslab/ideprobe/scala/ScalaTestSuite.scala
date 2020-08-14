package org.virtuslab.ideprobe.scala

import java.nio.file.Files

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.dependencies.{InternalPlugins, Plugin}
import org.virtuslab.ideprobe.{IntegrationTestSuite, IntelliJFixture, ProbeDriver, RunningIntelliJFixture}

class ScalaTestSuite extends IntegrationTestSuite {

  /**
   * The presence of .idea can prevent automatic import of gradle project
   */
  protected def deleteIdeaSettings(intelliJ: RunningIntelliJFixture): Unit = {
    val path = intelliJ.workspace.resolve(".idea")
    if (Files.exists(path)) path.delete()
  }

  val scalaProbePlugin: Plugin = InternalPlugins.bundle("ideprobe-scala")

  override protected def transformFixture(fixture: IntelliJFixture): IntelliJFixture = {
    fixture
      .withPlugin(scalaProbePlugin)
      .withAfterIntelliJInstall { (_, inteliJ) =>
        // The scala-library from ideprobe plugin causes conflict with the scala-library from
        // scala plugin. This is why we delete one of them. We declare the scala-library as an
        // optional dependency with config file probePlugin/src/main/resources/META-INF/scala-plugin.xml
        // so that ideprobe plugin can be loaded regardless of the missing scala-library.
        inteliJ.paths.plugins.resolve("ideprobe/lib/scala-library.jar").delete()
      }
  }

  implicit def scalaProbeDriver(driver: ProbeDriver): ScalaProbeDriver = ScalaProbeDriver(driver)
}
