package org.virtuslab.ideprobe.config

import pureconfig.ConfigReader
import pureconfig.generic.auto._

import org.virtuslab.ideprobe.ConfigFormat

case class IdeProbeConfig(
    intellij: IntellijConfig,
    workspace: Option[WorkspaceConfig],
    resolvers: DependenciesConfig.Resolvers,
    driver: DriverConfig,
    paths: PathsConfig
)

object IdeProbeConfig extends ConfigFormat {
  implicit val format: ConfigReader[IdeProbeConfig] = exportReader[IdeProbeConfig].instance

}
