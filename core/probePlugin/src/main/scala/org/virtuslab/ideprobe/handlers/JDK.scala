package org.virtuslab.ideprobe.handlers

import com.intellij.openapi.projectRoots.ProjectJdkTable

object JDK extends IntelliJApi {
  def preconfigure(): Unit = runOnUISync {
    ProjectJdkTable.getInstance().preconfigure()
  }
}
