package org.virtuslab.ideprobe

import com.intellij.openapi.module.Module
import org.virtuslab.ideprobe.protocol.ModuleRef
import org.virtuslab.ideprobe.protocol.ProjectRef

object ProbePluginExtensions {

  final implicit class ModuleExtension(module: Module) {
    def toRef: ModuleRef = {
      val project = module.getProject
      ModuleRef(module.getName, ProjectRef(project.getName))
    }
  }

}
