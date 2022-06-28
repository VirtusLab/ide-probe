package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

/**
 * Regardless of the condition it waits for a constant specified `duration` once.
 */
class ConstantWaiting private (
    duration: FiniteDuration,
    waitCondition: WaitCondition
) extends WaitLogic {

  def this(duration: FiniteDuration) = {
    this(duration, _ => WaitDecision.Done)
  }

  override def await(driver: ProbeDriver): Unit = {
    waitCondition(driver)
    sleep(duration)
  }

  override def and(waitCondition: WaitCondition): ConstantWaiting = {
    new ConstantWaiting(duration, driver => this.waitCondition(driver) && waitCondition(driver))
  }

  override def or(waitCondition: WaitCondition): ConstantWaiting = {
    new ConstantWaiting(duration, driver => this.waitCondition(driver) || waitCondition(driver))
  }
}
