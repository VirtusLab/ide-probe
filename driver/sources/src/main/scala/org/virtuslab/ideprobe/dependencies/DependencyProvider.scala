package org.virtuslab.ideprobe
package dependencies

import java.nio.file.Path
import org.virtuslab.ideprobe.dependencies.Dependency._
import scala.collection.mutable
import scala.util.Try

final class IntelliJDependencyProvider(
    intelliJResolvers: Seq[DependencyResolver[IntelliJVersion]],
    resources: ResourceProvider
) {
  def fetch(intelliJ: IntelliJVersion): Path = {
    val noResolversError = Try[Path](error("Dependency resolver list is empty"))
    intelliJResolvers
      .foldLeft(noResolversError) { (result, resolver) =>
        result.orElse(Try(resolve(intelliJ, resolver)))
      }
      .get
  }

  private def resolve(
      intelliJ: IntelliJVersion,
      resolver: DependencyResolver[IntelliJVersion]
  ): Path = {
    resolver.resolve(intelliJ) match {
      case Artifact(uri) =>
        resources.get(uri)
      case other =>
        throw new Exception(s"Couldn't resolve $intelliJ from $other")
    }
  }
}

final class PluginDependencyProvider(
    pluginResolvers: Seq[DependencyResolver[Plugin]],
    resources: ResourceProvider
) {
  def fetch(plugin: Plugin): Path = {
    val noResolversError = Try[Path](error("Dependency resolver list is empty"))
    pluginResolvers
      .foldLeft(noResolversError) { (result, resolver) =>
        result.orElse(Try(resolve(plugin, resolver)))
      }
      .get
  }

  private def resolve(plugin: Plugin, resolver: DependencyResolver[Plugin]): Path = {
    resolver.resolve(plugin) match {
      case Artifact(uri) =>
        resources.get(uri)
      case Sources(id, config) =>
        DependencyProvider.builders.get(id) match {
          case Some(builder) => builder.build(config, resources)
          case None =>
            val message =
              s"No builder found for id: '$id'. Available builders are ${DependencyProvider.builders.keySet}. " +
                s"Make sure you registered your builder with org.virtuslab.ideprobe.dependencies.DependencyProvider#registerBuilder"
            throw new RuntimeException(message)
        }
      case dependency =>
        throw new Exception(s"Couldn't resolve $plugin from $dependency")
    }
  }
}

final class DependencyProvider(
    intelliJDependencyProvider: IntelliJDependencyProvider,
    pluginDependencyProvider: PluginDependencyProvider
) {
  def fetch(intelliJ: IntelliJVersion): Path = {
    intelliJDependencyProvider.fetch(intelliJ)
  }

  def fetch(plugin: Plugin): Path = {
    pluginDependencyProvider.fetch(plugin)
  }
}

object DependencyProvider {
  private[dependencies] val builders = mutable.Map[Id, DependencyBuilder]()

  def registerBuilder(builder: DependencyBuilder): Unit = {
    builders += (builder.id -> builder)
  }
}
