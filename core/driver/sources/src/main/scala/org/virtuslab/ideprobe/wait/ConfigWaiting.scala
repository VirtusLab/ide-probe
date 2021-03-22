package org.virtuslab.ideprobe
package wait

/**
 * Allows to read the `WaitLogic` from config at given config path,
 * and provide a fallback object.
 * */
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
    val resolved = driver.config
      .getOrElse[WaitLogic](configPath, fallback)
    delayedTransformation(resolved)
  }

  override def and(waitCondition: WaitCondition): ConfigWaiting = {
    new ConfigWaiting(
      configPath,
      fallback,
      resolvedLogic => delayedTransformation(resolvedLogic).and(waitCondition)
    )
  }

  override def or(waitCondition: WaitCondition): ConfigWaiting = {
    new ConfigWaiting(
      configPath,
      fallback,
      resolvedLogic => delayedTransformation(resolvedLogic).or(waitCondition)
    )
  }

}
