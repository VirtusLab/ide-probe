package org.virtuslab.ideprobe.benchmark
package report

trait BenchmarkReporter {
  def report(name: String, results: Seq[BenchmarkResult]): Unit
}
