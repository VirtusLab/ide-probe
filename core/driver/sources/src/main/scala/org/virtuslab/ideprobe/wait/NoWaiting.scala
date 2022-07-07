package org.virtuslab.ideprobe
package wait

/**
 * Does not wait.
 */
object NoWaiting extends WaitLogic {
  override def await(driver: ProbeDriver): Unit = ()

  override def and(waitCondition: WaitCondition): NoWaiting.type = this

  override def or(waitCondition: WaitCondition): NoWaiting.type = this
}
