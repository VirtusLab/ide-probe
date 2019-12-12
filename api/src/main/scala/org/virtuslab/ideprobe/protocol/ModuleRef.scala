package org.virtuslab.ideprobe.protocol

final case class ModuleRef(name: String, project: ProjectRef = ProjectRef.Default)

object ModuleRef {
  def apply(name: String, project: String): ModuleRef = {
    ModuleRef(name, ProjectRef(project))
  }
}
