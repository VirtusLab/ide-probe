package org.virtuslab.ideprobe.handlers

import org.virtuslab.ideprobe.Config

object ProbeConfig {
  private var config = Config.Empty

  def initialize(configContent: String): Unit = {
    config = Config.fromString(configContent)
  }

  def get(): Config = config
}
