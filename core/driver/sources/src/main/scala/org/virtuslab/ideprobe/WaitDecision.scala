package org.virtuslab.ideprobe

sealed trait WaitDecision {
  def waitingDone: Boolean
  def needToWait: Boolean = !waitingDone
  def message: Option[String]
  def ||(other: WaitDecision): WaitDecision = WaitDecision.or(this, other)
  def &&(other: WaitDecision): WaitDecision = WaitDecision.and(this, other)
}

object WaitDecision {

  def and(a: WaitDecision, b: WaitDecision): WaitDecision = {
    if (a.waitingDone && b.waitingDone) {
      Done
    } else {
      KeepWaiting(a.message.orElse(b.message))
    }
  }

  def or(a: WaitDecision, b: WaitDecision): WaitDecision = {
    if (a.waitingDone || b.waitingDone) {
      Done
    } else {
      KeepWaiting(a.message.orElse(b.message))
    }
  }

  case object Done extends WaitDecision {
    override def waitingDone: Boolean = true
    override def message: Option[String] = None
  }

  case class KeepWaiting(message: Option[String]) extends WaitDecision {
    override def waitingDone: Boolean = false
  }

  object KeepWaiting {
    def apply(message: String): KeepWaiting = new KeepWaiting(Some(message))
    def apply(): KeepWaiting = new KeepWaiting(None)
  }

}
