package org.virtuslab.ideprobe.pants.protocol

import java.nio.file.Path
import org.virtuslab.ideprobe.ConfigFormat
import org.virtuslab.ideprobe.jsonrpc.JsonRpc.Method.Request
import org.virtuslab.ideprobe.protocol.ProjectRef
import org.virtuslab.ideprobe.protocol.ModuleRef
import pureconfig.generic.auto._

object PantsEndpoints extends ConfigFormat {
  val ImportPantsProject =
    Request[(Path, PantsProjectSettingsChangeRequest), ProjectRef]("pants/project/import")

  val GetPantsProjectSettings =
    Request[ProjectRef, PantsProjectSettings]("pants/project/settings/get")

  val ChangePantsProjectSettings =
    Request[(ProjectRef, PantsProjectSettingsChangeRequest), Unit]("pants/project/settings/change")

  val GetPythonFacets = Request[ModuleRef, Seq[PythonFacet]]("python/module/facets/get")

}
