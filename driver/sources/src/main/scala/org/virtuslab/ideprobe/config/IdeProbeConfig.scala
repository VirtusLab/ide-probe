package org.virtuslab.ideprobe.config

import org.virtuslab.ideprobe.{ConfigFormat, EndpointsConfig}
import pureconfig.ConfigReader
import pureconfig.generic.auto._

case class IdeProbeConfig(
    intellij: IntellijConfig = IntellijConfig(),
    workspace: Option[WorkspaceConfig] = None,
    resolvers: DependenciesConfig.Resolvers = DependenciesConfig.Resolvers(),
    driver: DriverConfig = DriverConfig(),
    endpoints: EndpointsConfig = EndpointsConfig()
)

object IdeProbeConfig extends ConfigFormat {
  implicit val format: ConfigReader[IdeProbeConfig] = exportReader[IdeProbeConfig].instance
}
