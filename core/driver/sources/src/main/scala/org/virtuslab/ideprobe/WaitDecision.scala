package org.virtuslab.ideprobe

sealed trait WaitDecision {
  def needToWait: Boolean
  def message: Option[String]
  def ||(other: WaitDecision): WaitDecision = WaitDecision.or(this, other)
  def &&(other: WaitDecision): WaitDecision = WaitDecision.and(this, other)
}

object WaitDecision {

  def and(a: WaitDecision, b: WaitDecision): WaitDecision = {
    if (a.needToWait && b.needToWait) {
      KeepWaiting(a.message.orElse(b.message))
    } else {
      Done
    }
  }

  def or(a: WaitDecision, b: WaitDecision): WaitDecision = {
    if (a.needToWait || b.needToWait) {
      KeepWaiting(a.message.orElse(b.message))
    } else {
      Done
    }
  }

  case object Done extends WaitDecision {
    override def needToWait: Boolean = false
    override def message: Option[String] = None
  }

  case class KeepWaiting(message: Option[String]) extends WaitDecision {
    override def needToWait: Boolean = true
  }

  object KeepWaiting {
    def apply(message: String): KeepWaiting = new KeepWaiting(Some(message))
    def apply(): KeepWaiting = new KeepWaiting(None)
  }

}
