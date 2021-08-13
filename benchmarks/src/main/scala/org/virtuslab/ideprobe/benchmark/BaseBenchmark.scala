package org.virtuslab.ideprobe.benchmark

abstract class BaseBenchmark(val name: String, val numberOfWarmups: Int, val numberOfRuns: Int) {

  final def runOpt(): Option[BenchmarkResult] = {
    runOpt(new BenchmarkRunner(name, numberOfWarmups, numberOfRuns))
  }

  protected def runOpt(runner: BenchmarkRunner): Option[BenchmarkResult]
}

abstract class Benchmark(
    name: String,
    numberOfWarmups: Int,
    numberOfRuns: Int
) extends BaseBenchmark(name, numberOfWarmups, numberOfRuns) {

  protected final def runOpt(runner: BenchmarkRunner): Option[BenchmarkResult] = {
    Some(run(runner))
  }

  protected def run(runner: BenchmarkRunner): BenchmarkResult
}
