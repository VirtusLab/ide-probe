package org.virtuslab.ideprobe
package wait

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Extension of basic waiting logic for unstable conditions.
 *
 * Problem:
 * If for example we were waiting for all background tasks to
 * finish, with basic waiting, the check might happen when
 * all tasks completed, but after a moment another task might
 * start, triggering other tasks etc. Generally many background
 * tasks may appear and disappear during e.g. project import,
 * and there may be short periods without tasks at all, which
 * doesn't mean that the import is complete.
 *
 * Algorithm:
 * The algorithm checks the wait condition periodically, like
 * in `BasicWaiting`, when it seems like condition is met and
 * waiting would be done, there is an extra ensure period.
 * It has constant length, defined by `ensurePeriod` parameter.
 * During this period with frequency specified by `ensureFrequency`,
 * the condition is checked. If it holds through whole ensure period,
 * the waiting is complete, if at any point it turns out it does not
 * hold, the algorithm goes back to the basic waiting.
 * To give more detail, the example values of parameters may explain
 * it more. `basicCheckFrequency` would be 5 seconds, this will handle
 * most of waiting for long tasks that last for minutes. Then, if
 * waiting seems to be complete, algorithm would trigger 2 seconds
 * of `ensurePeriod` with `ensureFrequency` of 50 millis. This means
 * that tasks will be probed for 2 seconds every 50 millis to reduce
 * the chance that some task would "avoid" being noticed. If anything
 * is detected, algorithm goes back to waiting every 5 seconds, otherwise
 * waiting completes.
 * */
class UnstableConditionWaiting(
    basicCheckFrequency: FiniteDuration,
    ensurePeriod: FiniteDuration,
    ensureFrequency: FiniteDuration,
    atMost: FiniteDuration,
    waitCondition: WaitCondition
) extends WaitLogic
    with ThrottledLogging {

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

  override def and(waitCondition: WaitCondition): UnstableConditionWaiting = {
    new UnstableConditionWaiting(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      driver => this.waitCondition(driver) && waitCondition(driver)
    )
  }

  override def or(waitCondition: WaitCondition): UnstableConditionWaiting = {
    new UnstableConditionWaiting(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      driver => this.waitCondition(driver) || waitCondition(driver)
    )
  }
}
