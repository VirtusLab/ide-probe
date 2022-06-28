package org.virtuslab.ideprobe.config

final case class CheckConfig(errors: Boolean = false, ignoreErrorsWithMessageContaining: Seq[String] = Nil, freezes: Boolean = false)

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig(errors = false, freezes = false)
}
