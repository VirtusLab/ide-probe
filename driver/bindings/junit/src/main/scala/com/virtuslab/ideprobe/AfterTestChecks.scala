package com.virtuslab.ideprobe
import com.virtuslab.ideprobe.ide.intellij.CheckConfig

object AfterTestChecks {
  def apply(config: CheckConfig, probe: ProbeDriver): Unit = {
    val e = new Exception("Test failed due to postcondition failures")

    ErrorValidator(config, probe.errors).foreach(e.addSuppressed)
    FreezeValidator(config, probe.freezes).foreach(e.addSuppressed)

    if (e.getSuppressed.nonEmpty) throw e
  }
}
