package org.virtuslab.ideprobe.benchmark

import org.virtuslab.ideprobe.benchmark.report.BenchmarkReporter

case class BenchmarkSuite(name: String, benchmarks: Seq[BaseBenchmark]) {
  def run(reporter: BenchmarkReporter): Unit = {
    println(s"Running benchmark suite: $name")
    val results = benchmarks.flatMap(_.runOpt())
    reporter.report(name, results)
  }
}
