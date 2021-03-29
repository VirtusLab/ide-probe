package org.virtuslab.ideprobe.wait

/**
 * Utility that allows to attempt executing multiple times.
 * Code will execute until it succeeds for first time.
 *
 * val closeTip = new DoOnlyOnce(closeTipOfTheDay())
 * waitLogic.doWhileWaiting {
 *   closeTip.attempt()
 * }
 *
 * In the case above, `closeTipOfTheDay`, throws if the window is not there
 * or it fails to close it for some other reason. It will be retried during
 * waiting, until the window is actually closed and after it succeeds, the
 * logic for finding the component will not be executed anymore to avoid
 * wasting time, as queries can be time costly.
 * */
class DoOnlyOnce(action: => Unit) {
  private var doneSuccessfully = false

  def isSuccessful: Boolean = doneSuccessfully

  def attempt(): Unit = {
    if (!doneSuccessfully) {
      try {
        action
        doneSuccessfully = true
      } catch {
        case _: Exception =>
      }
    }
  }
}

class DoOnlyOnceLazyInit {
  private var doOnce: DoOnlyOnce = _

  def isSuccessful: Boolean = doOnce.isSuccessful

  def attempt(action: => Unit): Unit = {
    if (doOnce == null) {
      doOnce = new DoOnlyOnce(action)
    }
    doOnce.attempt()
  }
}
