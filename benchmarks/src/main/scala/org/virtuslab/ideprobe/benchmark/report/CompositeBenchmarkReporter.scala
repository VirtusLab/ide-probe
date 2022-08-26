package org.virtuslab.ideprobe.benchmark
package report

class CompositeBenchmarkReporter[A](reporters: Seq[BenchmarkReporter[A]]) extends BenchmarkReporter[A] {
  override def report(suite: String, results: Seq[BenchmarkResult[A]]): Unit = {
    reporters.foreach(_.report(suite, results))
  }
}
