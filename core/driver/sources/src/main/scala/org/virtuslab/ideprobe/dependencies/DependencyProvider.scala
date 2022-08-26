package org.virtuslab.ideprobe
package dependencies

import java.nio.file.Path

import scala.collection.mutable
import scala.util.Try

import org.virtuslab.ideprobe.dependencies.Dependency._

final class JbrDependencyProvider(
    resolvers: Seq[DependencyResolver[Path]],
    resources: ResourceProvider
) extends BaseDependencyProvider[Path](resolvers, resources)

final class IntelliJDependencyProvider(
    resolvers: Seq[DependencyResolver[IntelliJVersion]],
    resources: ResourceProvider
) extends BaseDependencyProvider[IntelliJVersion](resolvers, resources)

final class PluginDependencyProvider(
    pluginResolvers: Seq[DependencyResolver[Plugin]],
    resources: ResourceProvider
) extends BaseDependencyProvider[Plugin](pluginResolvers, resources) {

  override protected def resolve(plugin: Plugin, resolver: DependencyResolver[Plugin]): Option[Path] = {
    Some {
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
          error(s"Couldn't resolve $plugin from $dependency")
      }
    }
  }
}

final case class DependencyProvider(
    intelliJ: IntelliJDependencyProvider,
    plugin: PluginDependencyProvider,
    jbr: JbrDependencyProvider
)

object DependencyProvider {
  private[dependencies] val builders = mutable.Map[Id, DependencyBuilder]()

  def registerBuilder(builder: DependencyBuilder): Unit = {
    builders += (builder.id -> builder)
  }
}

abstract class BaseDependencyProvider[A](
    resolvers: Seq[DependencyResolver[A]],
    resources: ResourceProvider
) {

  def fetchOpt(key: A): Option[Path] = {
    val noResolvers = Option.empty[Path]
    resolvers
      .foldLeft(noResolvers) { (result, resolver) =>
        result.orElse(Try(resolve(key, resolver)).toOption.flatten)
      }
  }

  def fetch(key: A): Path = {
    val noResolversError = Try[Path](error("Dependency resolver list is empty"))
    resolvers
      .foldLeft(noResolversError) { (result, resolver) =>
        result.orElse(Try(resolve(key, resolver).get))
      }
      .get
  }

  protected def resolve(
      key: A,
      resolver: DependencyResolver[A]
  ): Option[Path] = {
    resolver.resolve(key) match {
      case Artifact(uri) =>
        Option(resources.get(uri))
      case Missing => None
      case other =>
        error(s"Couldn't resolve $key from $other")
    }
  }
}
