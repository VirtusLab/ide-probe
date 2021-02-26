package org.virtuslab.ideprobe.protocol

final case class TestsRunResult(suites: Seq[TestSuite]) {
  def isSuccess: Boolean = suites.forall(_.tests.forall(run => TestStatus.OkStatuses.contains(run.status)))
}

final case class TestSuite(name: String, tests: Seq[TestRun])

final case class TestRun(name: String, durationMs: Long, status: TestStatus)

sealed trait TestStatus

object TestStatus {
  val OkStatuses: Set[TestStatus] = Set(Passed, Ignored)

  case object Passed extends TestStatus
  case class Failed(errorMessage: String) extends TestStatus
  case object Ignored extends TestStatus
}
