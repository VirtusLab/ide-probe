package com.virtuslab.ideprobe.dependencies

import com.virtuslab.ideprobe.Id

abstract class DependencyBuilder(val id: Id) {
  def build(repository: SourceRepository, resources: ResourceProvider): Resource
}
