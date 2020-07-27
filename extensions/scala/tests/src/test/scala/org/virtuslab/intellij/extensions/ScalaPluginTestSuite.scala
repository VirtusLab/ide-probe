package org.virtuslab.intellij.extensions

import org.virtuslab.ideprobe.IntegrationTestSuite
import org.virtuslab.ideprobe.dependencies.DependencyProvider

class ScalaPluginTestSuite extends IntegrationTestSuite {
  DependencyProvider.registerBuilder(ScalaPluginBuilder)
}
