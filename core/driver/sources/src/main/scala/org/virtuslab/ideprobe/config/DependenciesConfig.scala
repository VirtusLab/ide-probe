package org.virtuslab.ideprobe.config

object DependenciesConfig {
  case class Resolvers(
      intellij: IntelliJ = IntelliJ(Seq.empty),
      plugins: Plugins = Plugins(None)
  )

  case class IntelliJ(
      repositories: Seq[String]
  )

  case class Plugins(
      repository: Option[PluginRepository]
  )

  case class PluginRepository(uri: String)

}
