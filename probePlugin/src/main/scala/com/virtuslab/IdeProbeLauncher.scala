package com.virtuslab

import com.intellij.ide.ApplicationInitializedListener
import com.virtuslab.log.IdeaLogInterceptor
import com.virtuslab.log.NotificationsInterceptor

/**
 * Starts the IdeProbe on startup
 */
final class IdeProbeLauncher extends ApplicationInitializedListener {
  override def componentsInitialized(): Unit = {
    IdeaLogInterceptor.inject()
    NotificationsInterceptor.inject()
    WindowMonitor.inject()
    IdeProbeService().start()
  }
}
