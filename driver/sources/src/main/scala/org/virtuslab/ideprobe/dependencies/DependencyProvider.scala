package org.virtuslab.ideprobe.dependencies

import java.nio.file.Path
import org.virtuslab.ideprobe.Id
import org.virtuslab.ideprobe.dependencies.Dependency._
import scala.collection.mutable

final class IntelliJDependencyProvider(
    intelliJResolver: DependencyResolver[IntelliJVersion],
    resources: ResourceProvider
) {
  def fetch(intelliJ: IntelliJVersion): Path = {
    intelliJResolver.resolve(intelliJ) match {
      case Artifact(uri) =>
        resources.get(uri)
      case other =>
        throw new Exception(s"Couldn't resolve $intelliJ from $other")
    }
  }
}

final class PluginDependencyProvider(
    pluginResolver: DependencyResolver[Plugin],
    resources: ResourceProvider
) {
  def fetch(plugin: Plugin): Path = {
    pluginResolver.resolve(plugin) match {
      case Artifact(uri) =>
        resources.get(uri)
      case Sources(id, repository) =>
        DependencyProvider.builders.get(id) match {
          case Some(builder) => builder.build(repository, resources)
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
