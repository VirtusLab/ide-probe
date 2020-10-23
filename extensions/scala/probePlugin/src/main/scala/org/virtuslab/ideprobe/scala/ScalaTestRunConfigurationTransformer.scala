package org.virtuslab.ideprobe.scala

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration
import org.virtuslab.ideprobe.RunConfigurationTransformer

class ScalaTestRunConfigurationTransformer extends RunConfigurationTransformer {
  override def transform(runConfiguration: RunnerAndConfigurationSettings): RunnerAndConfigurationSettings = {
    runConfiguration.getConfiguration match {
      case scalaConfig: ScalaTestRunConfiguration =>
        scalaConfig.testConfigurationData.setUseSbt(true)
        scalaConfig.testConfigurationData.setUseUiWithSbt(true)
      case _ =>
    }
    runConfiguration
  }
}
