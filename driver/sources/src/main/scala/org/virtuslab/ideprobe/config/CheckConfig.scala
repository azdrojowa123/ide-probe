package org.virtuslab.ideprobe.config

final case class CheckConfig(errors: Boolean = false, freezes: Boolean = false)

object CheckConfig {
  val Disabled: CheckConfig = CheckConfig(errors = false, freezes = false)
}
