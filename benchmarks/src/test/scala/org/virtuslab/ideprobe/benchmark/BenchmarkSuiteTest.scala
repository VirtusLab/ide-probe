package org.virtuslab.ideprobe.benchmark

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import org.virtuslab.ideprobe.benchmark.report.BenchmarkReporter

@RunWith(classOf[JUnit4])
final class BenchmarkSuiteTest {

  class TestBenchmark[A](name: String, customData: A) extends Benchmark[A](name, 0, 1) {
    override protected def run(runner: BenchmarkRunner): BenchmarkResult[A] = {
      runner.run { measure =>
        measure {
          customData
        }
      }
    }
  }

  @Test
  def simpleBenchmarkWithCustomData(): Unit = {

    val result = "test result"
    val benchmarks = List(new TestBenchmark("test", result))
    BenchmarkSuite("open-project", benchmarks).run(new BenchmarkReporter[String] {
      override def report(name: String, results: Seq[BenchmarkResult[String]]): Unit = {
        assertEquals(Seq(result), results.head.customData)
      }
    })
  }

  @Test
  def simpleBenchmarkWithoutCustomData(): Unit = {

    val benchmarks = List(new TestBenchmark[Unit]("test", ()))
    BenchmarkSuite[Unit]("open-project", benchmarks).run(new BenchmarkReporter[Unit] {
      override def report(name: String, results: Seq[BenchmarkResult[Unit]]): Unit = {
        assertEquals(Seq.empty[String], results.head.customData.toList)
      }
    })
  }
}
