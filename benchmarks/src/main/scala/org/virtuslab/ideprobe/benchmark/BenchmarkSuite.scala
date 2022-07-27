package org.virtuslab.ideprobe.benchmark

import org.virtuslab.ideprobe.benchmark.report.BenchmarkReporter

case class BenchmarkSuite[A](name: String, benchmarks: Seq[BaseBenchmark[A]]) {
  def run(reporter: BenchmarkReporter[A]): Unit = {
    println(s"Running benchmark suite: $name")
    val results = benchmarks.flatMap(_.runOpt())
    reporter.report(name, results)
  }
}
