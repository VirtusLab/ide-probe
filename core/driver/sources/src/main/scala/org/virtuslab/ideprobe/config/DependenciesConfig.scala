package org.virtuslab.ideprobe.config

object DependenciesConfig {
  case class Resolvers(
      intellij: IntelliJ,
      plugins: Plugins,
      jbr: Jbr,
      retries: Int
  )

  case class IntelliJ(repositories: Seq[String])

  case class Plugins(repository: PluginRepository)

  case class PluginRepository(uri: String)

  case class Jbr(repositories: Seq[String])
}
