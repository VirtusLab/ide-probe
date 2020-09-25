package org.virtuslab.ideprobe.dependencies

import java.nio.file.Path
import org.virtuslab.ideprobe.Id

abstract class DependencyBuilder(val id: Id) {
  def build(repository: SourceRepository, resources: ResourceProvider): Path
}
