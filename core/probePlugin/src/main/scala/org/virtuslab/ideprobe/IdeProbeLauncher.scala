package org.virtuslab.ideprobe

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationInfo
import org.apache.log4j.{LogManager, Logger}
import org.virtuslab.ideprobe.log.IdeaLogInterceptor
import org.virtuslab.ideprobe.log.NotificationsInterceptor
import org.virtuslab.ideprobe.log.legacy.{IdeaLogInterceptor => LegacyIdeaLogInterceptor}

/**
 * Starts the IdeProbe on startup
 */
final class IdeProbeLauncher extends ApplicationInitializedListener {

  private val logger: Logger = LogManager.getLogger(classOf[IdeProbeLauncher])

  override def componentsInitialized(): Unit = {
    println("> inject")
    logger.error(s"> Intejct")
    logger.error(s"IJ version:${ApplicationInfo.getInstance().getMajorVersion.toInt}")
    if (ApplicationInfo.getInstance().getMajorVersion.toInt <= 2021)
      LegacyIdeaLogInterceptor.inject()
    else
      IdeaLogInterceptor.inject()
    NotificationsInterceptor.inject()
    WindowMonitor.inject()
    IdeProbeService().start()
    logger.info("> IdeProbeLauncher")
  }
}
