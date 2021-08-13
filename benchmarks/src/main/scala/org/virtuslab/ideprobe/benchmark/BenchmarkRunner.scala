package org.virtuslab.ideprobe.benchmark

import scala.concurrent.duration._

class BenchmarkRunner(name: String, numberOfWarmups: Int, numberOfRuns: Int) {

  class Measure {
    private var totalTime = Duration.Zero

    private[BenchmarkRunner] def measuredTime: FiniteDuration = totalTime

    def apply[A](action: => A): A = {
      val (result, time) = timed { action }
      totalTime += time
      result
    }

    private def timed[A](action: => A): (A, FiniteDuration) = {
      val start = System.currentTimeMillis()
      val result = action
      val end = System.currentTimeMillis()
      val total = (end - start).millis
      (result, total)
    }
  }

  def run(run: Measure => Unit): BenchmarkResult = {
    for (_ <- 1 to numberOfWarmups) {
      val measure = new Measure
      run(measure)
    }

    val results = (1 to numberOfRuns).map { iteration =>
      withRetry(2) {
        println(s"Running iteration: $iteration")
        val measure = new Measure
        run(measure)
        measure.measuredTime
      }
    }

    BenchmarkResult(name, numberOfWarmups, numberOfRuns, results, Map.empty)
  }

  private def withRetry[A](tries: Int)(action: => A): A = {
    try {
      action
    } catch {
      case _: Exception if tries > 1 =>
        println("Retrying...")
        withRetry(tries - 1)(action)
    }
  }
}
