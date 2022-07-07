package org.virtuslab.ideprobe
package wait

import scala.concurrent.duration.FiniteDuration

import pureconfig.ConfigReader
import pureconfig.generic.auto._

import org.virtuslab.ideprobe.wait.WaitLogicFactory._

object WaitLogicConfigFormat extends ConfigFormat {
  private sealed trait WaitLogicConfig

  private case class Constant(delay: FiniteDuration) extends WaitLogicConfig

  private case object NoWaiting extends WaitLogicConfig

  private case class EmptyBackgroundTasks(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ) extends WaitLogicConfig

  private case class EmptyNamedBackgroundTasks(
      basicCheckFrequency: FiniteDuration = DefaultCheckFrequency,
      ensurePeriod: FiniteDuration = DefaultEnsurePeriod,
      ensureFrequency: FiniteDuration = DefaultEnsureFrequency,
      atMost: FiniteDuration = DefaultAtMost
  ) extends WaitLogicConfig

  val format: ConfigReader[WaitLogic] = ConfigReader[WaitLogicConfig].map {
    case NoWaiting       => WaitLogic.none
    case Constant(delay) => WaitLogic.constant(delay)
    case EmptyBackgroundTasks(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost) =>
      WaitLogic.emptyBackgroundTasks(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost)
    case EmptyNamedBackgroundTasks(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost) =>
      WaitLogic.emptyNamedBackgroundTasks(basicCheckFrequency, ensurePeriod, ensureFrequency, atMost)
  }

}
