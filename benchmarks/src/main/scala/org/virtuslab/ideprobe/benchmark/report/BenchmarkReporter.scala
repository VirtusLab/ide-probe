package org.virtuslab.ideprobe.benchmark
package report

trait BenchmarkReporter[A] {
  def report(name: String, results: Seq[BenchmarkResult[A]]): Unit
}
