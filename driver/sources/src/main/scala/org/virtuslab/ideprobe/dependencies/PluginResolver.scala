package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.dependencies.Plugin.Bundled

object PluginResolver {
  val Official = PluginResolver("https://plugins.jetbrains.com/plugin/download")

  def apply(uri: String): DependencyResolver[Plugin] = {
    new Resolver(uri)
  }

  def from(configuration: DependenciesConfig.Plugins): DependencyResolver[Plugin] = {
    configuration.repository.map(repo => PluginResolver(repo.uri)).getOrElse(Official)
  }

  private final class Resolver(uri: String) extends DependencyResolver[Plugin] {
    override def resolve(plugin: Plugin): Dependency = {
      plugin match {
        case Plugin.Direct(uri) =>
          Dependency.Artifact(uri)

        case Plugin.FromSources(id, repository) =>
          Dependency.Sources(id, repository)

        case Plugin.Versioned(id, version, Some(channel)) =>
          Dependency(s"$uri?pluginId=$id&version=$version&channel=$channel")

        case Plugin.Versioned(id, version, None) =>
          Dependency(s"$uri?pluginId=$id&version=$version")

        case Bundled(bundle) =>
          val resource = getClass.getResource("/" + bundle)
          if (resource == null) {
            throw new Error(s"Bundle $bundle is not available on classpath")
          } else {
            Dependency.Artifact(resource.toURI)
          }
      }
    }
  }
}
