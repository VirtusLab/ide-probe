package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration._

/**
 * Utility class that helps with handling `atMost` parameter of `WaitLogic`. It starts the clock on object creation.
 * Then the used class calls `check()` periodically. If the time limit exceeds, the method will throw an error.
 */
class WaitLimit(limit: FiniteDuration) {
  private var startTime: Long = System.currentTimeMillis()

  def restart(): Unit = {
    startTime = System.currentTimeMillis()
  }

  def check(): Unit = {
    check(s"Waiting limit of $limit exceeded.")
  }

  def check(errorMessage: String): Unit = {
    if (isExceeded) error(errorMessage)
  }

  def isExceeded: Boolean = {
    val now = System.currentTimeMillis()
    val duration = (now - startTime).millis
    duration >= limit
  }

}
