package org.virtuslab.ideprobe
package wait

import scala.annotation.tailrec
import scala.concurrent.duration._

/**
 * Waits for a condition, checking it in equal intervals.
 * */
class BasicWaiting(
    checkFrequency: FiniteDuration,
    atMost: FiniteDuration,
    waitCondition: WaitCondition
) extends WaitLogic
    with ThrottledLogging {

  override def await(driver: ProbeDriver): Unit = {
    val limit = new WaitLimit(atMost)

    @tailrec def doWait(): Unit = {
      limit.check()
      val decision = waitCondition(driver)
      if (decision.needToWait) {
        decision.message.foreach(log)
        sleep(checkFrequency)
        doWait()
      }
    }

    doWait()
  }

  override def and(waitCondition: WaitCondition): BasicWaiting = {
    new BasicWaiting(
      checkFrequency,
      atMost,
      driver => this.waitCondition(driver) && waitCondition(driver)
    )
  }

  override def or(waitCondition: WaitCondition): BasicWaiting = {
    new BasicWaiting(
      checkFrequency,
      atMost,
      driver => this.waitCondition(driver) || waitCondition(driver)
    )
  }
}
