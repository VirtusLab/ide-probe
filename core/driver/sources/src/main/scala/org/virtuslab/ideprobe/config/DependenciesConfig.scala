package org.virtuslab.ideprobe.config

object DependenciesConfig {
  case class Resolvers(
      intellij: IntelliJ = IntelliJ(None),
      plugins: Plugins = Plugins(None)
  )

  case class IntelliJ(
      repository: Option[IntellijMavenRepository]
  )

  case class IntellijMavenRepository(
      uri: String,
      group: String,
      artifact: String
  )

  case class Plugins(
      repository: Option[PluginRepository]
  )

  case class PluginRepository(uri: String)

}
