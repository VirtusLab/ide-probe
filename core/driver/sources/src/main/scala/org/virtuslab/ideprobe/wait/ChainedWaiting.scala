package org.virtuslab.ideprobe
package wait

class ChainedWaiting(logics: Seq[WaitLogic]) extends WaitLogic {
  override def await(driver: ProbeDriver): Unit = {
    logics.foreach(_.await(driver))
  }

  override def and(waitCondition: ProbeDriver => WaitDecision): ChainedWaiting = {
    map(_.and(waitCondition))
  }

  override def or(waitCondition: ProbeDriver => WaitDecision): ChainedWaiting = {
    map(_.or(waitCondition))
  }

  private def map(f: WaitLogic => WaitLogic): ChainedWaiting = {
    new ChainedWaiting(logics.map(f))
  }
}
