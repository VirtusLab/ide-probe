package org.virtuslab.ideprobe
package wait

object NoWaiting extends WaitLogic {
  override def await(driver: ProbeDriver): Unit = ()

  override def and(waitCondition: ProbeDriver => WaitDecision): NoWaiting.type = this

  override def or(waitCondition: ProbeDriver => WaitDecision): NoWaiting.type = this
}
