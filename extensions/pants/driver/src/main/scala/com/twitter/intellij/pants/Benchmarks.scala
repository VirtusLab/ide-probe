package com.twitter.intellij.pants

import java.util.concurrent.Executors
import org.virtuslab.ideprobe.{IntelliJFixture, RunningIntelliJFixture}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}

object Benchmarks {

  private implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def withRunningIntelliJ[A](
    expectedDuration: Duration,
    fixture: IntelliJFixture
  )(
    run: RunningIntelliJFixture => A
  ): Unit = {
    withRunningIntelliJ(expectedDuration, expectedDuration * 2, fixture)(run)
  }

  def withRunningIntelliJ[A](
    expectedDuration: Duration,
    maxDuration: Duration,
    fixture: IntelliJFixture
  )(
    run: RunningIntelliJFixture => A
  ): Unit = {
    fixture.run { intelliJ =>
      try {
        val (duration, result) = timed {
          val future = Future(run(intelliJ))
          Await.result(future, maxDuration)
        }
        if (duration > expectedDuration) {
          throw new AssertionError(
            s"Benchmark did not complete within expected duration: $expectedDuration. It took ${prettyPrint(duration)}."
          )
        } else {
          println(
            s"Benchmark completed successfully in ${prettyPrint(duration)} (Expected less than $expectedDuration).")
        }
        result
      } catch {
        case _: TimeoutException =>
          throw new AssertionError(
            s"Benchmark did not complete within max duration: $maxDuration and was aborted. It was expected to complete within $expectedDuration")
      }
    }
  }

  private def prettyPrint(duration: Duration): String = {
    val javaDuration = java.time.Duration.ofMillis(duration.toMillis)
    javaDuration.toString.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase
  }

  def timed[A](block: => A): (Duration, A) = {
    val start = System.currentTimeMillis()
    val result = block
    val end = System.currentTimeMillis()
    val total = (end - start).millis
    (total, result)
  }
}
