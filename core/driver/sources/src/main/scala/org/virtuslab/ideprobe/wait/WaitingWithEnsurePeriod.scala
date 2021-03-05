package org.virtuslab.ideprobe
package wait

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}

class WaitingWithEnsurePeriod(
    basicCheckFrequency: FiniteDuration,
    ensurePeriod: FiniteDuration,
    ensureFrequency: FiniteDuration,
    atMost: FiniteDuration,
    waitCondition: ProbeDriver => WaitDecision
) extends WaitLogic
    with BackoffLogging {

  override def await(driver: ProbeDriver): Unit = {
    val limit = new WaitLimit(atMost)

    @tailrec
    def makeSureConditionHolds(probingTime: FiniteDuration): Boolean = {
      if (probingTime <= Duration.Zero) {
        true
      } else {
        limit.check()
        sleep(ensureFrequency)
        val decision = waitCondition(driver)
        if (decision.needToWait) {
          false
        } else {
          makeSureConditionHolds(probingTime - ensureFrequency)
        }
      }
    }

    @tailrec def doWait(): Unit = {
      limit.check()
      val decision = waitCondition(driver)
      if (decision.needToWait) {
        decision.message.foreach(log)
        sleep(basicCheckFrequency)
        doWait()
      } else {
        if (!makeSureConditionHolds(probingTime = ensurePeriod)) {
          doWait()
        }
      }
    }

    doWait()
  }

  override def and(waitCondition: ProbeDriver => WaitDecision): WaitingWithEnsurePeriod = {
    new WaitingWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      driver => this.waitCondition(driver) && waitCondition(driver)
    )
  }

  override def or(waitCondition: ProbeDriver => WaitDecision): WaitingWithEnsurePeriod = {
    new WaitingWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      driver => this.waitCondition(driver) || waitCondition(driver)
    )
  }
}
