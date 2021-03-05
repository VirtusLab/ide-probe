package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration._

trait ThrottledLogging {
  private lazy val throttle = new Throttle(maxPrintFrequency)

  protected def maxPrintFrequency: FiniteDuration = 1.second

  protected def log(string: String): Unit = {
    throttle.execute(println(string))
  }
}
