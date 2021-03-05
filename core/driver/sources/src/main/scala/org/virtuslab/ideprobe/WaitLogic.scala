package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.wait._
import pureconfig.ConfigReader
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait WaitLogic {
  def await(driver: ProbeDriver): Unit

  def and(waitCondition: ProbeDriver => WaitDecision): WaitLogic

  def or(waitCondition: ProbeDriver => WaitDecision): WaitLogic

  def doWhileWaiting(maxFrequency: FiniteDuration)(code: => Unit): WaitLogic = {
    val backoff = new ExecutionBackoff(maxFrequency)
    or { _ =>
      backoff.execute { code }
      WaitDecision.Done
    }
  }

  def doWhileWaiting(code: => Unit): WaitLogic = {
    doWhileWaiting(5.seconds)(code)
  }
}

object WaitLogic extends WaitLogicFactory {
  implicit val format: ConfigReader[WaitLogic] = WaitLogicConfigFormat.format
}
