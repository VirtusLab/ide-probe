package org.virtuslab.ideprobe.benchmark

import scala.concurrent.duration._

case class BenchmarkResult[A](
    name: String,
    numberOfWarmups: Int,
    numberOfRuns: Int,
    measures: Seq[FiniteDuration],
    metadata: Map[String, String],
    customData: Seq[A] = Seq.empty) {

  def withMetadata(metadata: Map[String, String]): BenchmarkResult[A] = {
    copy(metadata = this.metadata ++ metadata)
  }

  val meanTime: Option[FiniteDuration] = {
    optionWhen(measures.nonEmpty) {
      measures.fold(Duration.Zero)(_ + _) / measures.size
    }
  }

  val medianTime: Option[Duration] = optionWhen(measures.nonEmpty) {
    val (lower, upper) = measures.sorted.splitAt(measures.size / 2)
    if (measures.size % 2 == 0) (lower.last + upper.head) / 2.0 else upper.head
  }

  val stdev: Option[FiniteDuration] = {
    meanTime.filter(_ => measures.size >= 2).map { mean =>
      val squares = measures
          .map(_.toMillis)
          .map(time => (time - mean.toMillis) * (time - mean.toMillis))
        .sum

      math.sqrt(squares / (measures.size - 1).toDouble).millis
    }
  }

  private def optionWhen[A](condition: Boolean)(code: => A): Option[A] = {
    if (condition) Some(code) else None
  }
}
