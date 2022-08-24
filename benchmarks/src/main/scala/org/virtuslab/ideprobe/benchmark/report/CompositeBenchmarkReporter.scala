package org.virtuslab.ideprobe.benchmark
package report

class CompositeBenchmarkReporter(reporters: Seq[BenchmarkReporter]) extends BenchmarkReporter {
  override def report[A](suite: String, results: Seq[BenchmarkResult[A]]): Unit = {
    reporters.foreach(_.report(suite, results))
  }
}
