package com.virtuslab.ideprobe.dependencies

import java.net.URI
import com.virtuslab.ideprobe.Id

sealed trait Dependency
object Dependency {
  case class Artifact(uri: URI) extends Dependency
  case class Sources(id: Id, repository: SourceRepository) extends Dependency

  def apply(path: String): Dependency = Artifact(URI.create(path))
}
