package org.virtuslab.ideprobe

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.extensions.ExtensionPointName

trait RunConfigurationTransformer {
  def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings
}

object RunConfigurationTransformer {
  val EP_NAME =
    ExtensionPointName.create[RunConfigurationTransformer]("org.virtuslab.ideprobe.runConfigurationTransformer")

  def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings = {
    EP_NAME
      .getExtensions()
      .foldLeft(runConfiguration)((handler, contributor) => contributor.transform(handler))
  }
}
