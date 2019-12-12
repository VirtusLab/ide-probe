package com.virtuslab.ideprobe.dependencies

import com.virtuslab.ideprobe.Id
import com.virtuslab.ideprobe.dependencies.Dependency._
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
        DependencyProvider.builders(id).build(repository, resources)

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
