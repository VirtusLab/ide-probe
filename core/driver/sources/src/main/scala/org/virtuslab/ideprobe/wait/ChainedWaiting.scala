package org.virtuslab.ideprobe
package wait

/**
 * Executes each supplied `WaitLogic` sequentially.
 * */
class ChainedWaiting(logics: Seq[WaitLogic]) extends WaitLogic {
  override def await(driver: ProbeDriver): Unit = {
    logics.foreach(_.await(driver))
  }

  override def and(waitCondition: WaitCondition): ChainedWaiting = {
    map(_.and(waitCondition))
  }

  override def or(waitCondition: WaitCondition): ChainedWaiting = {
    map(_.or(waitCondition))
  }

  private def map(f: WaitLogic => WaitLogic): ChainedWaiting = {
    new ChainedWaiting(logics.map(f))
  }
}
