package org.virtuslab.ideprobe.benchmark

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.virtuslab.ideprobe.benchmark.report.{BenchmarkReporter, ConsoleBenchmarkReporter}
import org.virtuslab.ideprobe.{Assertions, IdeProbeFixture, IntelliJFixture, WorkspaceTemplate}
import org.virtuslab.ideprobe.ide.intellij.IntelliJProvider
import org.virtuslab.ideprobe.protocol.ProjectRef


@RunWith(classOf[JUnit4])
final class BenchmarkSuiteTest extends IdeProbeFixture with Assertions {
  private val intelliJProvider = IntelliJProvider.Default
  private val probeTestPlugin = ProbeTestPlugin.bundled(intelliJProvider.version)

  private val fixture = IntelliJFixture()
    .withPlugin(probeTestPlugin)
    .enableExtensions

  @Test
  def simpleBenchamrk(): Unit = {

    class TestBenchmark(name: String) extends Benchmark[String](name, 0, 1) {
      override protected def run(runner: BenchmarkRunner): BenchmarkResult[String] = {
        runner.run { measure =>
          fixture
            .copy(workspaceProvider = WorkspaceTemplate.FromResource("OpenProjectTest"))
            .run { intelliJ =>
              val expectedProjectName = "empty-project"
              val projectPath = intelliJ.workspace.resolve(expectedProjectName)
              measure {
                "test result"
              }
            }
        }
      }
      val benchmarks = List(new TestBenchmark("test"))
      BenchmarkSuite("open-project", benchmarks).run(new BenchmarkReporter {
        override def report[A](name: String, results: Seq[BenchmarkResult[A]]): Unit = {
          assertEquals(List("test result"), results.head.customData)
          assertEquals(List("test result"), results.head.customData)
        }
      }
      )
    }
  }

}
