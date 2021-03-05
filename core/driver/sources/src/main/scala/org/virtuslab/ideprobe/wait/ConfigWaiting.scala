package org.virtuslab.ideprobe
package wait

class ConfigWaiting private (
    configPath: String,
    fallback: WaitLogic,
    delayedTransformation: WaitLogic => WaitLogic
) extends WaitLogic {

  def this(
      configPath: String,
      fallback: WaitLogic
  ) = {
    this(configPath, fallback, identity)
  }

  override def await(driver: ProbeDriver): Unit = {
    resolve(driver).await(driver)
  }

  def resolve(driver: ProbeDriver): WaitLogic = {
    driver.config
      .getOrElse[WaitLogic](configPath, fallback)
  }

  override def and(waitCondition: ProbeDriver => WaitDecision): ConfigWaiting = {
    new ConfigWaiting(
      configPath,
      fallback,
      resolvedLogic => delayedTransformation(resolvedLogic).and(waitCondition)
    )
  }

  override def or(waitCondition: ProbeDriver => WaitDecision): ConfigWaiting = {
    new ConfigWaiting(
      configPath,
      fallback,
      resolvedLogic => delayedTransformation(resolvedLogic).or(waitCondition)
    )
  }

}
