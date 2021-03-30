package org.virtuslab.ideprobe.scala

import org.virtuslab.ideprobe.Extensions._
import org.virtuslab.ideprobe.{IdeProbeFixture, ProbeDriver}
import org.virtuslab.ideprobe.dependencies.DependencyProvider
import org.virtuslab.ideprobe.dependencies.InternalPlugins

import scala.language.implicitConversions

trait ScalaPluginExtension { this: IdeProbeFixture =>
  DependencyProvider.registerBuilder(ScalaPluginBuilder)

  registerFixtureTransformer(InternalPlugins.installCrossVersionPlugin("ideprobe-scala"))
  registerFixtureTransformer { fixture =>
    fixture
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
