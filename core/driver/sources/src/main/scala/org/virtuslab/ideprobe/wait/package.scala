package org.virtuslab.ideprobe

import scala.concurrent.duration.FiniteDuration

package object wait {
  def sleep(duration: FiniteDuration): Unit = {
    Thread.sleep(duration.toMillis)
  }

  type WaitCondition = ProbeDriver => WaitDecision
}
