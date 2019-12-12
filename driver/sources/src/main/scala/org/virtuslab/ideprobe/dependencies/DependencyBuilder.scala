package org.virtuslab.ideprobe.dependencies

import org.virtuslab.ideprobe.Id

abstract class DependencyBuilder(val id: Id) {
  def build(repository: SourceRepository, resources: ResourceProvider): Resource
}
