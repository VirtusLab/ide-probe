package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

class ConstantWaiting private (
    duration: FiniteDuration,
    waitCondition: ProbeDriver => WaitDecision
) extends WaitLogic {

  def this(duration: FiniteDuration) = {
    this(duration, _ => WaitDecision.Done)
  }

  override def await(driver: ProbeDriver): Unit = {
    waitCondition(driver)
    sleep(duration)
  }

  override def and(waitCondition: ProbeDriver => WaitDecision): ConstantWaiting = {
    new ConstantWaiting(duration, driver => this.waitCondition(driver) && waitCondition(driver))
  }

  override def or(waitCondition: ProbeDriver => WaitDecision): ConstantWaiting = {
    new ConstantWaiting(duration, driver => this.waitCondition(driver) || waitCondition(driver))
  }
}
