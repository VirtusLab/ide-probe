package org.virtuslab.ideprobe.benchmark
package report

class CompositeBenchmarkReporter(reporters: Seq[BenchmarkReporter]) extends BenchmarkReporter {
  override def report(suite: String, results: Seq[BenchmarkResult]): Unit = {
    reporters.foreach(_.report(suite, results))
  }
}
