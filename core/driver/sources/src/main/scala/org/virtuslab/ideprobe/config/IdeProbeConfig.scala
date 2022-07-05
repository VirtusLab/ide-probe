package org.virtuslab.ideprobe.config

import pureconfig.CamelCase
import pureconfig.ConfigFieldMapping
import pureconfig.ConfigReader
import pureconfig.KebabCase
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import org.virtuslab.ideprobe.ConfigFormat

case class IdeProbeConfig(
    intellij: IntellijConfig = IntellijConfig(),
    workspace: Option[WorkspaceConfig] = None,
    resolvers: DependenciesConfig.Resolvers = DependenciesConfig.Resolvers(),
    driver: DriverConfig = DriverConfig(),
    paths: PathsConfig = PathsConfig()
)

object IdeProbeConfig extends ConfigFormat {
  implicit val format: ConfigReader[IdeProbeConfig] = exportReader[IdeProbeConfig].instance
  override implicit def hint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, KebabCase))

}
