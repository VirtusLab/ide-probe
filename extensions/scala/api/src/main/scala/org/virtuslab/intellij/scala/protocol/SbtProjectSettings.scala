package org.virtuslab.intellij.scala.protocol

import org.virtuslab.ideprobe.protocol.Setting

case class SbtProjectSettings(
    useSbtShellForImport: Boolean,
    useSbtShellForBuild: Boolean,
    allowSbtVersionOverride: Boolean
)

case class SbtProjectSettingsChangeRequest(
    useSbtShellForImport: Setting[Boolean] = Setting.Unchanged,
    useSbtShellForBuild: Setting[Boolean] = Setting.Unchanged,
    allowSbtVersionOverride: Setting[Boolean] = Setting.Unchanged
)
