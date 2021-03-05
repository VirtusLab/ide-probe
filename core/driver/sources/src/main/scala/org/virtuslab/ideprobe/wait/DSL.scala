package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

class BasicDSL(checkFrequency: FiniteDuration, atMost: FiniteDuration) {
  def apply(waitCondition: WaitCondition): BasicWaiting = {
    new BasicWaiting(checkFrequency, atMost, waitCondition)
  }

  def apply(waitCondition: => WaitDecision): BasicWaiting = {
    new BasicWaiting(checkFrequency, atMost, _ => waitCondition)
  }
}

class UnstableConditionDSL(
    basicCheckFrequency: FiniteDuration,
    ensurePeriod: FiniteDuration,
    ensureFrequency: FiniteDuration,
    atMost: FiniteDuration
) {

  def apply(waitCondition: WaitCondition): UnstableConditionWaiting = {
    new UnstableConditionWaiting(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      waitCondition
    )
  }

  def apply(waitCondition: => WaitDecision): UnstableConditionWaiting = {
    new UnstableConditionWaiting(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      _ => waitCondition
    )
  }
}
