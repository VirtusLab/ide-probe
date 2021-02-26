package org.virtuslab.ideprobe.handlers

import org.virtuslab.ideprobe.Config

object ProbeConfig {
  private var probeConfig = Config.Empty

  def initialize(configContent: String): Unit = {
    probeConfig = Config.fromString(configContent)
  }

  def initialize(config: Config): Unit = {
    probeConfig = config
  }

  def get(): Config = probeConfig
}
