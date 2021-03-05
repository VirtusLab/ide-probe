package org.virtuslab.ideprobe
package wait

import scala.annotation.tailrec
import scala.concurrent.duration._

class BasicWaiting(
    checkFrequency: FiniteDuration,
    atMost: FiniteDuration,
    waitCondition: ProbeDriver => WaitDecision
) extends WaitLogic
    with BackoffLogging {

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

  override def and(waitCondition: ProbeDriver => WaitDecision): BasicWaiting = {
    new BasicWaiting(
      checkFrequency,
      atMost,
      driver => this.waitCondition(driver) && waitCondition(driver)
    )
  }

  override def or(waitCondition: ProbeDriver => WaitDecision): BasicWaiting = {
    new BasicWaiting(
      checkFrequency,
      atMost,
      driver => this.waitCondition(driver) || waitCondition(driver)
    )
  }
}
