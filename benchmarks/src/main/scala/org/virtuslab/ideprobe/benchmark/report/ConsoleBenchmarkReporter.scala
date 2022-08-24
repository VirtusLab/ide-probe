package org.virtuslab.ideprobe.benchmark
package report

import scala.concurrent.duration._

class ConsoleBenchmarkReporter extends BenchmarkReporter {
  def report[A](suite: String, results: Seq[BenchmarkResult[A]]): Unit = {
    def toSeconds(time: Duration): String = (time.toMillis / 1e3).toString
    def toSecondsOpt(time: Option[Duration]): String = time.fold("None")(toSeconds)

    println(s"Suite: $suite")
    results.foreach { result =>
      val properties = Seq(
        "Benchmark name" -> result.name,
        "Num warmup runs" -> result.numberOfWarmups,
        "Num measured runs" -> result.numberOfRuns,
        "Mean running time" -> toSecondsOpt(result.meanTime),
        "Median running time" -> toSecondsOpt(result.medianTime),
        "Sample standard deviation of running times" -> toSecondsOpt(result.stdev),
        "Running times" -> result.measures.map(toSeconds).mkString(", ")
      )
      properties.foreach { case (name, value) => println(s"$name: $value") }
    }
    println()
    println()
  }
}
