package com.virtuslab.ideprobe

import scala.concurrent.duration._

trait Assertions {

  def assertDuration[A](min: FiniteDuration = 0.millis, max: Duration)(block: => A): A = {
    val start = System.nanoTime()
    val result = block
    val end = System.nanoTime()
    val actual = (end - start).nanos
    if (!(actual <= max && actual >= min)) {
      fail(s"Action took ${actual.toMillis} ms which is not between $min and $max")
    }
    result
  }

  def assertExists[A](seq: Seq[A])(predicate: A => Boolean): Unit = {
    val exists = seq.exists(predicate)
    if (!exists) {
      val message = s"Could not find expected element in ${renderSeq(seq)}"
      fail(message)
    }
  }

  def assertContains[A](seq: Seq[A])(expected: A*): Unit = {
    val missing = expected.filterNot(seq.contains)
    if (missing.nonEmpty) {
      val message =
        s"Following elements ${renderSeq(missing)} are missing in ${renderSeq(seq)}"
      fail(message)
    }
  }

  def renderSeq[A](s: Seq[A]): String = {
    if (s.size > 1) s.mkString("\nSeq(\n  ", "\n  ", "\n)\n") else s.mkString("Seq(", ", ", ")")
  }

  private def fail(message: String) = {
    throw new AssertionError(message)
  }

}
