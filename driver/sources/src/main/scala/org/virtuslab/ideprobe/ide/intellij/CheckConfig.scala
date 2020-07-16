package org.virtuslab.ideprobe.ide.intellij

final case class CheckConfig(errors: Boolean = false, freezes: Boolean = false)

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig(false, false)
}
