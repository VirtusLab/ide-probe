package org.virtuslab.ideprobe.dependencies

import java.net.URI
import org.virtuslab.ideprobe.{Config, Id}

sealed trait Dependency
object Dependency {
  case class Artifact(uri: URI) extends Dependency
  case class Sources(id: Id, config: Config) extends Dependency

  def apply(path: String): Dependency = Artifact(URI.create(path))
}
