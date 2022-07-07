package org.virtuslab.ideprobe.protocol

import pureconfig.ConfigConvert
import pureconfig.generic.semiauto.deriveConvert

import org.virtuslab.ideprobe.ConfigFormat

final case class ModuleRef(name: String, project: ProjectRef = ProjectRef.Default)

object ModuleRef extends ConfigFormat {
  def apply(name: String, project: String): ModuleRef = {
    ModuleRef(name, ProjectRef(project))
  }

  implicit val moduleRefConvert: ConfigConvert[ModuleRef] = deriveConvert[ModuleRef]
}
