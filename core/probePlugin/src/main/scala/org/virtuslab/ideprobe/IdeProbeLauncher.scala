package org.virtuslab.ideprobe

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationInfo
import org.virtuslab.ideprobe.log.IdeaLogInterceptor
import org.virtuslab.ideprobe.log.NotificationsInterceptor
import org.virtuslab.ideprobe.log.legacy.{IdeaLogInterceptor => LegacyIdeaLogInterceptor}

/**
 * Starts the IdeProbe on startup
 */
final class IdeProbeLauncher extends ApplicationInitializedListener {
  override def componentsInitialized(): Unit = {
    if (ApplicationInfo.getInstance().getMajorVersion.toInt <= 2021)
      LegacyIdeaLogInterceptor.inject()
    else
      IdeaLogInterceptor.inject()
    NotificationsInterceptor.inject()
    WindowMonitor.inject()
    IdeProbeService().start()
  }
}
