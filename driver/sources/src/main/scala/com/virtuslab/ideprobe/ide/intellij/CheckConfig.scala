package com.virtuslab.ideprobe.ide.intellij

final case class CheckConfig(errors: Boolean = false, freezes: Boolean = true)

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig(false, false)
}
