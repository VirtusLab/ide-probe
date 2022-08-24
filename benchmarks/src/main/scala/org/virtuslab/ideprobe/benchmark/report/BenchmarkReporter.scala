package org.virtuslab.ideprobe.benchmark
package report

trait BenchmarkReporter {
  def report[A](name: String, results: Seq[BenchmarkResult[A]]): Unit
}
