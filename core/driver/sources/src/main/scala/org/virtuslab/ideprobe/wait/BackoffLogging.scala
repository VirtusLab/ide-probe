package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration._

trait BackoffLogging {
  private lazy val backoff = new ExecutionBackoff(maxPrintFrequency)

  protected def maxPrintFrequency: FiniteDuration = 1.second

  protected def log(string: String): Unit = {
    backoff.execute(println(string))
  }
}
