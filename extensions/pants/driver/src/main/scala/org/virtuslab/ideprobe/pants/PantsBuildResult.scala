package org.virtuslab.ideprobe.pants

case class PantsBuildResult(status: PantsBuildResult.Status.Value, output: String) {
  def assertSuccess(): Unit =
    if (isFailed) throw new AssertionError(s"Build did not succeed:\n$output")
  def isSuccessful: Boolean = status == PantsBuildResult.Status.Passed
  def isFailed: Boolean = !isSuccessful
}

object PantsBuildResult {
  object Status extends Enumeration {
    val Passed, Failed, Timeout = Value
  }
}
