package org.virtuslab.ideprobe

import org.virtuslab.ideprobe.wait._
import pureconfig.ConfigReader
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait WaitLogic {
  def await(driver: ProbeDriver): Unit

  def and(waitCondition: WaitCondition): WaitLogic

  def or(waitCondition: WaitCondition): WaitLogic

  def doWhileWaiting(code: ProbeDriver => Unit): WaitLogic = {
    doWhileWaiting(5.seconds, code)
  }

  def doWhileWaiting(maxFrequency: FiniteDuration)(code: => Unit): WaitLogic = {
    doWhileWaiting(maxFrequency, _ => code)
  }

  def doWhileWaiting(code: => Unit): WaitLogic = {
    doWhileWaiting(5.seconds)(code)
  }

  def doWhileWaiting(maxFrequency: FiniteDuration, code: ProbeDriver => Unit): WaitLogic = {
    val throttle = new Throttle(maxFrequency)
    and { driver =>
      throttle.execute { code(driver) }
      WaitDecision.Done
    }
  }
}

object WaitLogic extends WaitLogicFactory {
  implicit val format: ConfigReader[WaitLogic] = WaitLogicConfigFormat.format
}
