package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.config.DependenciesConfig
import org.virtuslab.ideprobe.dependencies.Plugin._
import org.virtuslab.ideprobe.error

object PluginResolver {
  val Official: DependencyResolver[Plugin] =
    PluginResolver(IntelliJFixture.defaultIdeProbeConfig.resolvers.plugins.repository.uri)

  def apply(uri: String): DependencyResolver[Plugin] = {
    new Resolver(uri)
  }

  def fromConfig(configuration: DependenciesConfig.Plugins): DependencyResolver[Plugin] =
    PluginResolver(configuration.repository.uri)

  private final class Resolver(uri: String) extends DependencyResolver[Plugin] {

    private def getBundled(path: String) = {
      val resource = getClass.getResource(path)
      if (resource == null) {
        throw new Error(s"Bundle $path is not available on classpath")
      } else {
        Dependency.Artifact(resource.toURI)
      }
    }

    override def resolve(plugin: Plugin): Dependency = {
      plugin match {
        case Plugin.Direct(uri) =>
          Dependency.Artifact(uri)

        case Plugin.FromSources(id, config) =>
          Dependency.Sources(id, config)

        case Plugin.Versioned(id, version, Some(channel)) =>
          Dependency(s"$uri?pluginId=$id&version=$version&channel=$channel")

        case Plugin.Versioned(id, version, None) =>
          Dependency(s"$uri?pluginId=$id&version=$version")

        case BundledCrossVersion(name, scalaVersion, version) =>
          getBundled(s"/${name}_$scalaVersion-$version.zip")

        case Bundled(bundle) =>
          getBundled(s"/$bundle")

        case Empty() =>
          error("Attempt to resolve an empty plugin")
      }
    }
  }
}
