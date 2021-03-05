package org.virtuslab.ideprobe.wait

import scala.concurrent.duration.FiniteDuration

/**
 * Utility class that allows to execute code not more frequently
 * than `maxFrequency`.
 * */
class Throttle(maxFrequency: FiniteDuration) {

  private var lastExecutionTime = Option.empty[Long]

  def execute(block: => Unit): Unit = {
    val now = System.currentTimeMillis()
    lastExecutionTime match {
      case Some(last) =>
        if (now - last >= maxFrequency.toMillis) {
          lastExecutionTime = Some(now)
          block
        }
      case None =>
        lastExecutionTime = Some(now)
        block
    }
  }

}
