package org.virtuslab.ideprobe.benchmark

import scala.concurrent.duration._

case class BenchmarkResult(
    name: String,
    numberOfWarmups: Int,
    numberOfRuns: Int,
    results: Seq[FiniteDuration],
    metadata: Map[String, String],
    customData: Seq[Any]) {

  def withMetadata(metadata: Map[String, String]): BenchmarkResult = {
    copy(metadata = this.metadata ++ metadata)
  }

  val meanTime: Option[FiniteDuration] = {
    optionWhen(results.nonEmpty) {
      results.fold(Duration.Zero)(_ + _) / results.size
    }
  }

  val medianTime: Option[Duration] = optionWhen(results.nonEmpty) {
    val (lower, upper) = results.sorted.splitAt(results.size / 2)
    if (results.size % 2 == 0) (lower.last + upper.head) / 2.0 else upper.head
  }

  val stdev: Option[FiniteDuration] = {
    meanTime.filter(_ => results.size >= 2).map { mean =>
      val squares = results
        .map(_.toMillis)
        .map(time => (time - mean.toMillis) * (time - mean.toMillis))
        .sum

      math.sqrt(squares / (results.size - 1).toDouble).millis
    }
  }

  private def optionWhen[T](condition: Boolean)(code: => T): Option[T] = {
    if (condition) Some(code) else None
  }
}
