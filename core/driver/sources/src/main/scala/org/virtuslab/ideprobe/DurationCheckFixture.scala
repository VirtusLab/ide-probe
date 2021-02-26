package org.virtuslab.ideprobe

import scala.concurrent.duration.Duration

trait DurationCheckFixture extends IdeProbeFixture {

  def checkDuration(configPrefix: String, benchmarkedAction: RunningIntelliJFixture => Unit): Unit = {
    val fixture = fixtureFromConfig()
    val expectedDuration = fixture.config[Duration](s"$configPrefix.benchmark.expectedDuration")
    val maxDuration = fixture.config
      .get[Duration](s"$configPrefix.benchmark.maxDuration")
      .getOrElse(expectedDuration * 2)
    DurationAssertions.withRunningIntelliJ(expectedDuration, maxDuration, fixture) { intelliJ =>
      benchmarkedAction(intelliJ)
    }
  }

}
