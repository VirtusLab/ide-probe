package org.virtuslab.ideprobe.benchmark

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.benchmark.report.BenchmarkReporter


@RunWith(classOf[JUnit4])
final class BenchmarkSuiteTest {

  @Test
  def simpleBenchamrk(): Unit = {

    class TestBenchmark(name: String) extends Benchmark(name, 0, 1) {
      override protected def run(runner: BenchmarkRunner): BenchmarkResult = {
        runner.run { measure =>
          val result = measure {
            "test result"
          }
          println(s"results:$result")
          result
        }
      }
    }
    val benchmarks = List(new TestBenchmark("test"))
    BenchmarkSuite("open-project", benchmarks).run(new BenchmarkReporter {
      override def report(name: String, results: Seq[BenchmarkResult]): Unit = {
        assertEquals(List("test result"), results.head.customData)
      }
    }
    )
  }

}
