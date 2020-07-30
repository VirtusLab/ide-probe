package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.Id
import org.virtuslab.ideprobe.dependencies.Dependency._
import scala.collection.mutable

final class DependencyProvider(
    intelliJResolver: DependencyResolver[IntelliJVersion],
    pluginResolver: DependencyResolver[Plugin],
    resources: ResourceProvider
) {
  def fetch(intelliJ: IntelliJVersion): Resource = {
    intelliJResolver.resolve(intelliJ) match {
      case Artifact(uri) =>
        resources.get(uri)

      case dependency =>
        throw new Exception(s"Couldn't resolve $intelliJ from $dependency")
    }
  }

  def fetch(plugin: Plugin): Resource = {
    pluginResolver.resolve(plugin) match {
      case Artifact(uri) =>
        resources.get(uri)

      case Sources(id, repository) =>
        DependencyProvider.builders.get(id) match {
          case Some(builder) => builder.build(repository, resources)
          case None =>
            val message = s"No builder found for id: '$id'. Available builders are ${DependencyProvider.builders.keySet}. " +
              s"Make sure you registered your builder with org.virtuslab.ideprobe.dependencies.DependencyProvider#registerBuilder"
            throw new RuntimeException(message)
        }

      case dependency =>
        throw new Exception(s"Couldn't resolve $plugin from $dependency")
    }
  }
}

object DependencyProvider {
  private val builders = mutable.Map[Id, DependencyBuilder]()

  def registerBuilder(builder: DependencyBuilder): Unit = {
    builders += (builder.id -> builder)
  }
}
