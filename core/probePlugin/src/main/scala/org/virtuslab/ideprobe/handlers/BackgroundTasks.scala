package org.virtuslab.ideprobe.handlers

import java.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

import org.virtuslab.ideprobe.Extensions._

object BackgroundTasks extends IntelliJApi {

  def currentBackgroundTasks(): Seq[String] = {
    val progressManager = ProgressManager.getInstance
    val indicators = progressManager.invoke[util.List[ProgressIndicator]]("getCurrentIndicators")()
    indicators.asScala.toSeq.map(indicator => Option(indicator.toString).getOrElse("<unknown>"))
  }

}
