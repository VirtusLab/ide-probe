package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration._

class WaitLimit(limit: FiniteDuration) {
  private var startTime: Long = System.currentTimeMillis()

  def restart(): Unit = {
    startTime = System.currentTimeMillis()
  }

  def check(): Unit = {
    check(s"Waiting limit of $limit exceeded.")
  }

  def check(errorMessage: String): Unit = {
    val now = System.currentTimeMillis()
    val duration = (now - startTime).millis
    if (duration >= limit) {
      error(errorMessage)
    }
  }

}
