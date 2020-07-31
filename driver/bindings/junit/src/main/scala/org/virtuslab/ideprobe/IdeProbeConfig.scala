package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.dependencies.DependenciesConfig
import org.virtuslab.ideprobe.ide.intellij.DriverConfig
import org.virtuslab.ideprobe.ide.intellij.IntellijConfig
import pureconfig.ConfigReader
import pureconfig.generic.auto._

case class IdeProbeConfig(
    intellij: IntellijConfig = IntellijConfig(),
    workspace: Option[WorkspaceConfig] = None,
    resolvers: DependenciesConfig.Resolvers = DependenciesConfig.Resolvers(),
    driver: DriverConfig = DriverConfig(),
    test: TestConfig = TestConfig()
)

object IdeProbeConfig extends ConfigFormat {
  implicit val format: ConfigReader[IdeProbeConfig] = exportReader[IdeProbeConfig].instance
}

case class TestConfig(modules: Seq[String] = Seq.empty)

object TestConfig {
  val Empty = TestConfig()
}
