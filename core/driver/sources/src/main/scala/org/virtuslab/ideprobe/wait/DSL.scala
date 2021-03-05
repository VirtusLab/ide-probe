package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

class BasicDSL(checkFrequency: FiniteDuration, atMost: FiniteDuration) {
  def apply(waitCondition: ProbeDriver => WaitDecision): BasicWaiting = {
    new BasicWaiting(checkFrequency, atMost, waitCondition)
  }

  def apply(waitCondition: => WaitDecision): BasicWaiting = {
    new BasicWaiting(checkFrequency, atMost, _ => waitCondition)
  }
}

class EnsurePeriodDSL(
    basicCheckFrequency: FiniteDuration,
    ensurePeriod: FiniteDuration,
    ensureFrequency: FiniteDuration,
    atMost: FiniteDuration
) {

  def apply(waitCondition: ProbeDriver => WaitDecision): WaitingWithEnsurePeriod = {
    new WaitingWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      waitCondition
    )
  }

  def apply(waitCondition: => WaitDecision): WaitingWithEnsurePeriod = {
    new WaitingWithEnsurePeriod(
      basicCheckFrequency,
      ensurePeriod,
      ensureFrequency,
      atMost,
      _ => waitCondition
    )
  }
}
