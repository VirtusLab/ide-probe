package org.virtuslab.ideprobe.benchmark

abstract class BaseBenchmark[A](val name: String, val numberOfWarmups: Int, val numberOfRuns: Int) {

  final def runOpt(): Option[BenchmarkResult[A]] = {
    runOpt(new BenchmarkRunner(name, numberOfWarmups, numberOfRuns))
  }

  protected def runOpt(runner: BenchmarkRunner): Option[BenchmarkResult[A]]
}

abstract class Benchmark[A](
    name: String,
    numberOfWarmups: Int,
    numberOfRuns: Int
) extends BaseBenchmark[A](name, numberOfWarmups, numberOfRuns) {

  protected final def runOpt(runner: BenchmarkRunner): Option[BenchmarkResult[A]] = {
    Some(run(runner))
  }

  protected def run(runner: BenchmarkRunner): BenchmarkResult[A]
}
