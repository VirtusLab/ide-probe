package org.virtuslab.ideprobe.bazel

case class BazelBuildResult(isSuccessful: Boolean, output: String) {

  def assertSuccess(): Unit = {
    if (isFailed) throw new AssertionError(s"Build did not succeed:\n$output")
  }

  def isFailed: Boolean = !isSuccessful

}
